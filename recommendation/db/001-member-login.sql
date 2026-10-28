-- Run once against your existing MySQL database, after a backup. No changes to perfumes.
CREATE TABLE app_members (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 naver_subject VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
 UNIQUE KEY uq_member_naver (naver_subject)
);
CREATE TABLE member_saved_perfumes (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 member_id VARCHAR(36) NOT NULL,
 perfume_id BIGINT NOT NULL,
 UNIQUE KEY uq_member_perfume (member_id, perfume_id),
 CONSTRAINT fk_saved_member FOREIGN KEY (member_id) REFERENCES app_members(id) ON DELETE CASCADE,
 CONSTRAINT fk_saved_perfume FOREIGN KEY (perfume_id) REFERENCES perfumes(id) ON DELETE CASCADE
);
