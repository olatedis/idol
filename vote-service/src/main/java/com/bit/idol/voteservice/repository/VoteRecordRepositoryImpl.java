package com.bit.idol.voteservice.repository;

import com.bit.idol.voteservice.dto.MyVoteRecordDto;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.bit.idol.voteservice.entity.QCandidate.candidate;
import static com.bit.idol.voteservice.entity.QVote.vote;
import static com.bit.idol.voteservice.entity.QVoteRecord.voteRecord;

@RequiredArgsConstructor
public class VoteRecordRepositoryImpl implements VoteRecordRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<MyVoteRecordDto> findMyVoteRecords(int userId, Long groupId) {
        com.bit.idol.voteservice.entity.QVoteRecord qVoteRecord = voteRecord;
        com.bit.idol.voteservice.entity.QVote qVote = vote;
        com.bit.idol.voteservice.entity.QCandidate qCandidate = candidate;

        com.querydsl.core.BooleanBuilder builder = new com.querydsl.core.BooleanBuilder();
        builder.and(qVoteRecord.userId.eq(userId));
        
        if (groupId != null) {
            builder.and(qVote.targetGroupId.eq(groupId));
        }

        return queryFactory
                .select(Projections.fields(MyVoteRecordDto.class,
                        qVoteRecord.voteId,
                        qVote.title.as("voteTitle"),
                        qCandidate.number.as("candidateNumber"),
                        qCandidate.name.as("candidateName"),
                        qVoteRecord.votedAt
                ))
                .from(qVoteRecord)
                .join(qVoteRecord.vote, qVote)
                .join(qVoteRecord.candidate, qCandidate)
                .where(builder)
                .orderBy(qVoteRecord.votedAt.desc())
                .fetch();
    }
}
