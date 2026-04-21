package team.hotpotato.infrastructure.r2dbc.common;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDateTime;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BaseEntity {
    @Id
    @Column("id")
    private Long id;

	@LastModifiedDate
    @Column("updatedAt")
    private LocalDateTime updatedAt;

	@CreatedDate
    @Column("createdAt")
    private LocalDateTime createdAt;

    @Column("deleted")
    private Boolean deleted;

    @Column("deleted_at")
    private LocalDateTime deletedAt;

	public void delete() {
		this.deleted = true;
		this.deletedAt = LocalDateTime.now();
	}

	public void restore() {
		this.deleted = false;
		this.deletedAt = null;
	}

	public boolean isDeleted() {
		return Boolean.TRUE.equals(deleted);
	}
}

