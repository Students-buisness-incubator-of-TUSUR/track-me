package net.akarmanov.projectplace.domain;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user_photo")
public class UserPhoto {

  @Id
  @Column(nullable = false,
          updatable = false)
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(nullable = false)
  private String fileName;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  @Column(nullable = false)
  private byte[] photo;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id",
              nullable = false)
  private User user;

}