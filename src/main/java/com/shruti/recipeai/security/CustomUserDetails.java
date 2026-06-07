package com.shruti.recipeai.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Data
public class CustomUserDetails implements UserDetails {
    private Long id;
    private String email;
    private String password;

    @Override
    @NonNull
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    @NonNull
    public String getUsername() {
        return email;
    }

    // JsonIgnore to avoid getting password serialized into JSON anytime for security reasons
    @JsonIgnore
    @Override
    public String getPassword() {
        return password;
    }
}
