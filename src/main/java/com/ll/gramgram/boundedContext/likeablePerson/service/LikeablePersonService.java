package com.ll.gramgram.boundedContext.likeablePerson.service;

import com.ll.gramgram.base.rq.Rq;
import com.ll.gramgram.base.rsData.RsData;
import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import com.ll.gramgram.boundedContext.instaMember.service.InstaMemberService;
import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import com.ll.gramgram.boundedContext.likeablePerson.repository.LikeablePersonRepository;
import com.ll.gramgram.boundedContext.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeablePersonService {
    private final LikeablePersonRepository likeablePersonRepository;
    private final InstaMemberService instaMemberService;

    @Transactional
    public RsData<LikeablePerson> like(Member member, String username, int attractiveTypeCode) {
        if ( member.hasConnectedInstaMember() == false ) {
            return RsData.of("F-2", "먼저 본인의 인스타그램 아이디를 입력해야 합니다.");
        }

        if (member.getInstaMember().getUsername().equals(username)) {
            return RsData.of("F-1", "본인을 호감상대로 등록할 수 없습니다.");
        }

        InstaMember fromInstaMember = member.getInstaMember();
        InstaMember toInstaMember = instaMemberService.findByUsernameOrCreate(username).getData();

        LikeablePerson likeablePerson = LikeablePerson
                .builder()
                .fromInstaMember(member.getInstaMember()) // 호감을 표시하는 사람의 인스타 멤버
                .fromInstaMemberUsername(member.getInstaMember().getUsername()) // 중요하지 않음
                .toInstaMember(toInstaMember) // 호감을 받는 사람의 인스타 멤버
                .toInstaMemberUsername(toInstaMember.getUsername()) // 중요하지 않음
                .attractiveTypeCode(attractiveTypeCode) // 1=외모, 2=능력, 3=성격
                .build();

        // 호감을 표시한 사람의 인스타 아이디
        InstaMember loginedMember = likeablePerson.getFromInstaMember();
        String newLikeablePerson = likeablePerson.getToInstaMember().getUsername();

        long count = loginedMember
                .getFromLikeablePeople()
                .stream()
                .count();

        if (count >= 10) {
            return RsData.of("F-1", "호감 상대가 10명을 초과했습니다.");
        }

        LikeablePerson likeabledPerson = loginedMember
                .getFromLikeablePeople()
                .stream()
                .filter(lp -> lp.getToInstaMember().getUsername().equals(newLikeablePerson))
                .findFirst()
                .orElse(null);

        if (likeabledPerson != null) {
            return RsData.of("F-1", "%s님은 이미 호감상대로 등록되어 있습니다.".formatted(newLikeablePerson));
        }

        likeablePersonRepository.save(likeablePerson); // 저장

        // 호감을 표시한 사람의 인스타 아이디
        InstaMember loginedMember = likeablePerson.getFromInstaMember();
        String newLikeablePerson = likeablePerson.getToInstaMember().getUsername();
        LikeablePerson likeabledPerson = loginedMember
                .getFromLikeablePeople()
                .stream()
                .filter(lp -> lp.getToInstaMember().getUsername().equals(newLikeablePerson))
                .findFirst()
                .orElse(null);

        if (likeabledPerson != null) {
            String attractiveTypeDisplayName = likeabledPerson.getAttractiveTypeDisplayName(); //이전에 호감을 표시할 때 선택한 매력

            //다른 매력을 선택했다면
            if (likeablePerson.getAttractiveTypeDisplayName() != likeabledPerson.getAttractiveTypeDisplayName()) {
                likeabledPerson.setAttractiveTypeCode(likeablePerson.getAttractiveTypeCode()); //매력 변경
                likeablePersonRepository.delete(likeabledPerson); //이전에 호감을 표시한 사람 삭제

                return RsData.of("S-2", "%s님의 매력이 %s에서 %s(으)로 변경되었습니다.".formatted(newLikeablePerson, attractiveTypeDisplayName, likeablePerson.getAttractiveTypeDisplayName()));
            }

            //같은 매력을 선택했다면
            return RsData.of("F-1", "%s님은 이미 호감상대로 등록되어 있습니다.".formatted(newLikeablePerson));
        }

        // 너가 좋아하는 호감표시 생겼어.
        fromInstaMember.addFromLikeablePerson(likeablePerson);

        // 너를 좋아하는 호감표시 생겼어.
        toInstaMember.addToLikeablePerson(likeablePerson);

        return RsData.of("S-1", "입력하신 인스타유저(%s)를 호감상대로 등록되었습니다.".formatted(username), likeablePerson);
    }

    public List<LikeablePerson> findByFromInstaMemberId(Long fromInstaMemberId) {
        return likeablePersonRepository.findByFromInstaMemberId(fromInstaMemberId);
    }

    @Transactional
    public RsData delete(LikeablePerson likeablePerson) {
        likeablePersonRepository.delete(likeablePerson);

        String username = likeablePerson.getToInstaMember().getUsername();
        return RsData.of("S-1", "%s님에 대한 호감을 취소하였습니다.".formatted(username));
    }

    public RsData deleteRsData(Member member, LikeablePerson likeablePerson) {
        if (likeablePerson == null)
            return RsData.of("F-1", "이미 삭제되었습니다.");

        // 로그인한 사람의 인스타 아이디
        long memberInstaMemberId = member.getInstaMember().getId();
        // 호감을 표시한 사람의 인스타 아이디
        long fromInstaMemberId = likeablePerson.getFromInstaMember().getId();
        if (memberInstaMemberId != fromInstaMemberId)
            return RsData.of("F-2", "삭제 권한이 없습니다.");

        return RsData.of("S-1", "삭제가능합니다.");
    }

    public Optional<LikeablePerson> findById(Long id) {
        return likeablePersonRepository.findById(id);
    }
}
