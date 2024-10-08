package com.gongyeon.io.netkim.model.repository;

import com.gongyeon.io.netkim.model.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, Integer> {
    // id 중복체크
    boolean existsByEmail(String email);
    
    // member id로 찾기
    MemberEntity findByMemberIdx(long memberIdx);
    
    // email로 찾기
    MemberEntity findByEmail(String email);

    // member 중 등업 요청을 한 사람들의 정보를 조회하는 메서드
    @Query(value = "SELECT * FROM member WHERE role = 'MEMBER' AND certificate_img IS NOT NULL ORDER BY update_date", nativeQuery = true)
    List<MemberEntity> getLevelUpMembers();
}