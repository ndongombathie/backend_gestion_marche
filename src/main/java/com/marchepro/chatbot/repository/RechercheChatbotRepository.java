package com.marchepro.chatbot.repository;

import com.marchepro.chatbot.entity.RechercheChatbot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RechercheChatbotRepository extends JpaRepository<RechercheChatbot, Long> {

    Page<RechercheChatbot> findByUtilisateurIdOrderByDateRechercheDesc(Long utilisateurId, Pageable pageable);
}
