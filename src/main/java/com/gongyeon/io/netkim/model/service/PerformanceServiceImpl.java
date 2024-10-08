package com.gongyeon.io.netkim.model.service;

import com.gongyeon.io.netkim.model.entity.PerformanceEntity;
import com.gongyeon.io.netkim.model.repository.PerformanceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PerformanceServiceImpl implements PerformanceService {
    private final PerformanceRepository performanceRepository;

    @Value("${spring.kopis.key}")
    String key;

    public PerformanceServiceImpl(PerformanceRepository performanceRepository) {
        this.performanceRepository = performanceRepository;
    }

    @Override
    public List<PerformanceEntity> getAllPerformance() {
        return performanceRepository.findAll();
    }

    @Override
    @Transactional
    public List<String> getAllprfnm() {
        return performanceRepository.findPrfnmList();
    }

    @Override
    @Transactional
    public PerformanceEntity getDetail(String kopisId) throws NullPointerException {
        PerformanceEntity performance = performanceRepository.findByKopisId(kopisId);
        // 기존에 상세조회가 되어 있는 경우에는 Query를 던질 필요가 없지만, 그렇지 않다면 Query를 다시 던진다.
        // 던지는 방법에는 여러가지가 있지만, 어떤 방식으로 던질지에 대해서는 고민이 필요하다.
        if(performance == null || performance.getPrfnm() == null) {
            throw new NullPointerException();
        }
        if(performance.getEntrpsnm()==null) {
            updatePerformance(kopisId);
        }
        return performance;
    }

    @Override
    @Transactional
    public PerformanceEntity getDetailName(String prfnm) throws NullPointerException {
        PerformanceEntity performance = performanceRepository.findByPrfnm(prfnm);

        if(performance == null) {
            throw new NullPointerException();
        }
        System.out.println(performance);
        if(performance.getPrfcast() == null) {
            System.out.println("업데이트 중");
            try{
                performance = updatePerformance(performance.getKopisId());
            }catch(NullPointerException e) {
                System.out.println(e.getMessage());
            }
        }
        System.out.println("이름으로 공연 조회 : " +prfnm);
        return performance;
    }

    @Override
    public int insertPerformance() {
        int page=1;																								// 조회할 db 페이지값 지정
        int res=0;

        try{
            while(page<=30) {
                // XML 호출값 URL세팅
                StringBuilder apiQuery = new StringBuilder("http://www.kopis.or.kr/openApi/restful/pblprfr");				// URL
                apiQuery.append("?service=").append(key); 			// Service Key
                apiQuery.append("&").append(URLEncoder.encode("stdate", StandardCharsets.UTF_8)).append("=").append(URLEncoder.encode("20160101", StandardCharsets.UTF_8));	// 검색 시작일
                apiQuery.append("&").append(URLEncoder.encode("eddate", StandardCharsets.UTF_8)).append("=").append(URLEncoder.encode("20251231", StandardCharsets.UTF_8));	// 검색 종료일
                apiQuery.append("&").append(URLEncoder.encode("cpage", StandardCharsets.UTF_8)).append("=").append(URLEncoder.encode(Integer.toString(page), StandardCharsets.UTF_8)); 			// 페이지번호
                apiQuery.append("&").append(URLEncoder.encode("rows", StandardCharsets.UTF_8)).append("=").append(URLEncoder.encode("1000", StandardCharsets.UTF_8));			// 페이지당 목록 수
                apiQuery.append("&").append(URLEncoder.encode("shcate", StandardCharsets.UTF_8)).append("=").append(URLEncoder.encode("GGGA", StandardCharsets.UTF_8));		// 장르명(뮤지컬 고정)
                apiQuery.append("&").append(URLEncoder.encode("newsql", StandardCharsets.UTF_8)).append("=").append(URLEncoder.encode("Y", StandardCharsets.UTF_8)); 		// 신규생성 메시지
//                System.out.println(apiQuery);																	// Query문 테스트

                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(apiQuery.toString());

                // root tag 구하기
                doc.getDocumentElement().normalize();
//			        System.out.println("Root element: " + doc.getDocumentElement().getNodeName());
                // Root element: 공연예술통합전산망 기준 <dbs>가 나오면 정상!

                // 파싱할 tag
                NodeList nList = doc.getElementsByTagName("db");
//			        System.out.println("파싱할 리스트 수 : "+ nList.getLength());											        // Query에서 100개의 결과값을 요청했으므로, 100이 나와야 정상(rows의 value와 맞추기)
                if(nList == null || nList.getLength()==0)
                    break;
                for(int i=0;i<nList.getLength();i++) {
                    Node nNode=nList.item(i);
                    if(nNode.getNodeType()==Node.ELEMENT_NODE) {
                        Element eElement=(Element) nNode;
                        if(performanceRepository.existsByKopisId(getTagValue("mt20id", eElement))){
                            continue;
                        }
                        PerformanceEntity performance = PerformanceEntity.builder()
                                .kopisId(getTagValue("mt20id", eElement))
                                .prfnm(getTagValue("prfnm", eElement))
                                .prfdfrom(LocalDate.parse(getTagValue("prfpdfrom", eElement), DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                                .prfdto(LocalDate.parse(getTagValue("prfpdto", eElement), DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                                .fcltynm(getTagValue("fcltynm", eElement))
                                .poster(getTagValue("poster", eElement))
                                .openrun(getTagValue("openrun", eElement).equals("Y"))
                                .prfstate(getTagValue("prfstate", eElement))
                                .build();
                        performanceRepository.save(performance);
                        res++;
                    }
                }
                page++;
                System.out.println((page-1)+"쪽 db저장 완료, \n"+page+"쪽 DB저장 시작");
            }	//while문 끝
            System.out.println("추가된 공연 DB의 수: "+res);
        }catch(Exception E) {
            System.out.println("DB를 받아오던 중 오류 발생 : "+E);
        }
        return res;
    }

    @Override
    @Transactional
    public PerformanceEntity updatePerformance(String kopisId) {
        StringBuilder apiQuery = new StringBuilder("http://www.kopis.or.kr/openApi/restful/pblprfr/");				// URL
        try {
            // Update 할 객체
            PerformanceEntity performance = performanceRepository.findByKopisId(kopisId);

            // Parcing 할 db 조회
            apiQuery.append(kopisId);
            apiQuery.append("?service=").append(key); 			// Service Key
            apiQuery.append("&").append(URLEncoder.encode("newsql", StandardCharsets.UTF_8)).append("=").append(URLEncoder.encode("Y", StandardCharsets.UTF_8)); 		// 신규생성 메시지
            System.out.println(apiQuery);


            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(apiQuery.toString());

            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("db");
            System.out.println("파싱할 리스트 수 : "+ nList.getLength());
            Node nNode=nList.item(0);
            if(nNode.getNodeType()==Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;
                performance.setEntrpsnm(getTagValue("entrpsnm", eElement));
                performance.setPrfcast(getTagValue("prfcast", eElement));
                performance.setPrfruntime(getMinute(getTagValue("prfruntime", eElement)));
                performanceRepository.save(performance);
                return performance;
            }
        }catch(Exception E) {
            System.out.println("업데이트 중 오류 발생" + E.getMessage());
        }finally {
            System.out.println(apiQuery);
        }
        return null;
    }

    //tag값 가져오기
    private static String getTagValue(String tag, Element eElement) {
        NodeList nlList = eElement.getElementsByTagName(tag).item(0).getChildNodes();
        return nlList.item(0).getTextContent();
    }

    private int getMinute(String timeString) {
        // "시간"과 "분" 기준으로 시간과 분을 추출
        int hours = Integer.parseInt(timeString.substring(0, timeString.indexOf("시간")).trim());
        int minutes = Integer.parseInt(timeString.substring(timeString.indexOf(" ") + 1, timeString.indexOf("분")).trim());

        return (hours * 60) + minutes;
    }
}