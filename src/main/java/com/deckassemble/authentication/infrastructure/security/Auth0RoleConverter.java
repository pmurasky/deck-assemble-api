package com.deckassemble.authentication.infrastructure.security;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class Auth0RoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    List<String> permissions = jwt.getClaimAsStringList("permissions");
    if (permissions == null) {
      return Collections.emptyList();
    }
    return permissions.stream()
        .map(permission -> "ROLE_" + permission.toUpperCase())
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());
  }
}
