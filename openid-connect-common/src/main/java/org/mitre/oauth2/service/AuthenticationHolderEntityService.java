package org.mitre.oauth2.service;

import org.mitre.oauth2.model.AuthenticationHolderEntity;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

@SuppressWarnings("deprecation")
public interface AuthenticationHolderEntityService {

  AuthenticationHolderEntity create(OAuth2Authentication authn);

  void remove(AuthenticationHolderEntity holder);

  long clearOrphaned();

}
