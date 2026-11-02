package com.marchepro.security;

import com.marchepro.utilisateur.entity.Role;
import com.marchepro.utilisateur.entity.StatutUtilisateur;
import com.marchepro.utilisateur.entity.Utilisateur;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String motDePasse;
    private final Role role;
    private final StatutUtilisateur statut;
    private final String nomComplet;

    public UserPrincipal(Utilisateur utilisateur) {
        this.id = utilisateur.getId();
        this.email = utilisateur.getEmail();
        this.motDePasse = utilisateur.getMotDePasse();
        this.role = utilisateur.getRole();
        this.statut = utilisateur.getStatut();
        this.nomComplet = utilisateur.getNomComplet();
    }

    public Long getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public String getNomComplet() {
        return nomComplet;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return motDePasse;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return statut != StatutUtilisateur.INACTIF;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return statut == StatutUtilisateur.ACTIF || statut == StatutUtilisateur.EN_ATTENTE;
    }
}
