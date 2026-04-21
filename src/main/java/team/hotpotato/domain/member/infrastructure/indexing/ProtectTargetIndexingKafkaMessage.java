package team.hotpotato.domain.member.infrastructure.indexing;

record ProtectTargetIndexingKafkaMessage(
        Long jobId,
        String keyword,
        String protectTargetInfo
) {
}
