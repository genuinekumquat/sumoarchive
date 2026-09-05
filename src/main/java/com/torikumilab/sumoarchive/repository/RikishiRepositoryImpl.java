package com.torikumilab.sumoarchive.repository;

import com.torikumilab.sumoarchive.domain.dto.search.RikishiSearchRow;
import com.torikumilab.sumoarchive.domain.entity.constant.MatchType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RikishiRepositoryImpl implements RikishiSearchRepositoryCustom {
	private final EntityManager em;
	
	public RikishiRepositoryImpl(EntityManager em) {
		this.em = em;
	}
	
	private static final String MATCH_CTE = """
        WITH matches AS (
            SELECT r.id AS rikishi_id,
                   CASE
                       WHEN r.shikona_kr = :keyword OR r.shikona_jp = :keyword THEN 0
                       WHEN r.shikona_kr LIKE CONCAT(:keyword, '%')
                         OR r.shikona_jp LIKE CONCAT(:keyword, '%') THEN 1
                       ELSE 2
                   END AS match_rank
            FROM rikishi r
            WHERE r.shikona_kr LIKE CONCAT('%', :keyword, '%')
               OR r.shikona_jp LIKE CONCAT('%', :keyword, '%')

            UNION ALL

            SELECT h.rikishi_id, 3 AS match_rank
            FROM rikishi_shikona_history h
            WHERE h.shikona_kr LIKE CONCAT('%', :keyword, '%')
               OR h.shikona_jp LIKE CONCAT('%', :keyword, '%')
        ),
        best_match AS (
            SELECT rikishi_id, MIN(match_rank) AS match_rank
            FROM matches
            GROUP BY rikishi_id
        )
        """;
	
	@Override
	public Page<RikishiSearchRow> search(String keyword, Pageable pageable) {
		String dataSql = MATCH_CTE + """
            SELECT r.id,
                   r.name,
                   r.shikona_kr,
                   r.shikona_jp,
                   he.name_kr AS heya_name,
                   r.highest_rank,
                   YEAR(r.debut_date)   AS debut_year,
                   YEAR(r.retired_date) AS retirement_year,
                   r.is_active,
                   r.oyakata_name_kr,
                   bm.match_rank
            FROM rikishi r
            JOIN best_match bm ON bm.rikishi_id = r.id
            LEFT JOIN heya he ON he.id = r.heya_id
            ORDER BY r.is_active DESC, bm.match_rank ASC, r.id ASC
            LIMIT :limit OFFSET :offset
            """;
		
		String countSql = MATCH_CTE + "SELECT COUNT(*) FROM best_match";
		
		Query dataQuery = em.createNativeQuery(dataSql)
				.setParameter("keyword", keyword)
				.setParameter("limit", pageable.getPageSize())
				.setParameter("offset", pageable.getOffset());
		
		Query countQuery = em.createNativeQuery(countSql)
				.setParameter("keyword", keyword);
		
		@SuppressWarnings("unchecked")
		List<Object[]> rows = dataQuery.getResultList();
		long total = ((Number) countQuery.getSingleResult()).longValue();
		
		List<RikishiSearchRow> content = rows.stream()
				.map(RikishiRepositoryImpl::mapRow)
				.toList();
		
		return new PageImpl<>(content, pageable, total);
	}
	
	private static RikishiSearchRow mapRow(Object[] row) {
		return new RikishiSearchRow(
				((Number) row[0]).intValue(),
				(String) row[1],
				(String) row[2],
				(String) row[3],
				(String) row[4],
				(String) row[5],
				row[6] != null ? ((Number) row[6]).intValue() : null,
				row[7] != null ? ((Number) row[7]).intValue() : null,
				toBoolean(row[8]),
				(String) row[9],
				MatchType.fromRank(((Number) row[10]).intValue())
		);
	}
	
	private static Boolean toBoolean(Object value) {
		if (value == null) return null;
		if (value instanceof Boolean b) return b;
		if (value instanceof Number n) return n.intValue() != 0;
		return Boolean.parseBoolean(value.toString());
	}
}