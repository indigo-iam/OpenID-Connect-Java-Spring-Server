/*******************************************************************************
 * Copyright 2018 The MIT Internet Trust Consortium
 *
 * Portions copyright 2011-2013 The MITRE Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
/**
 *
 */
package org.mitre.oauth2.model;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.Transient;

import org.mitre.oauth2.model.convert.JWTStringConverter;
import org.mitre.openid.connect.model.ApprovedSite;
import org.mitre.uma.model.Permission;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessTokenJackson2Deserializer;
import org.springframework.security.oauth2.common.OAuth2AccessTokenJackson2Serializer;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;

import com.google.common.hash.Hashing;
import com.nimbusds.jwt.JWT;

/**
 * @author jricher
 *
 */
@SuppressWarnings("deprecation")
@Entity
@Table(name = "access_token")
@com.fasterxml.jackson.databind.annotation.JsonSerialize(
    using = OAuth2AccessTokenJackson2Serializer.class)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(
    using = OAuth2AccessTokenJackson2Deserializer.class)
public class OAuth2AccessTokenEntity implements OAuth2AccessToken {

  public static final String ID_TOKEN_FIELD_NAME = "id_token";

  private Long id;

  private ClientDetailsEntity client;

  private AuthenticationHolderEntity authenticationHolder;

  private JWT jwtValue;

  private String tokenValueHash;

  private Date expiration;

  private String tokenType = OAuth2AccessToken.BEARER_TYPE;

  private OAuth2RefreshTokenEntity refreshToken;

  private Set<String> scope;

  private Set<Permission> permissions;

  private ApprovedSite approvedSite;

  private Map<String, Object> additionalInformation = new HashMap<>();

  /**
   * Create a new, blank access token
   */
  public OAuth2AccessTokenEntity() {

  }

  /**
   * @return the id
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  public Long getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Get all additional information to be sent to the serializer as part of the token response. This
   * map is not persisted to the database.
   */
  @Override
  @Transient
  public Map<String, Object> getAdditionalInformation() {
    return additionalInformation;
  }

  /**
   * The authentication in place when this token was created.
   * 
   * @return the authentication
   */
  @ManyToOne
  @JoinColumn(name = "auth_holder_id")
  public AuthenticationHolderEntity getAuthenticationHolder() {
    return authenticationHolder;
  }

  /**
   * @param authentication the authentication to set
   */
  public void setAuthenticationHolder(AuthenticationHolderEntity authenticationHolder) {
    this.authenticationHolder = authenticationHolder;
  }

  /**
   * @return the client
   */
  @ManyToOne
  @JoinColumn(name = "client_id")
  public ClientDetailsEntity getClient() {
    return client;
  }

  /**
   * @param client the client to set
   */
  public void setClient(ClientDetailsEntity client) {
    this.client = client;
  }

  /**
   * Get the string-encoded value of this access token.
   */
  @Override
  @Transient
  public String getValue() {
    return jwtValue.serialize();
  }

  @Override
  @Basic
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  @Column(name = "expiration")
  public Date getExpiration() {
    return expiration;
  }

  public void setExpiration(Date expiration) {
    this.expiration = expiration;
  }

  @Override
  @Basic
  @Column(name = "token_type")
  public String getTokenType() {
    return tokenType;
  }

  public void setTokenType(String tokenType) {
    this.tokenType = tokenType;
  }

  @Override
  @ManyToOne
  @JoinColumn(name = "refresh_token_id")
  public OAuth2RefreshTokenEntity getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(OAuth2RefreshTokenEntity refreshToken) {
    this.refreshToken = refreshToken;
  }

  public void setRefreshToken(OAuth2RefreshToken refreshToken) {
    if (!(refreshToken instanceof OAuth2RefreshTokenEntity)) {
      throw new IllegalArgumentException("Not a storable refresh token entity!");
    }
    // force a pass through to the entity version
    setRefreshToken((OAuth2RefreshTokenEntity) refreshToken);
  }

  @Override
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(joinColumns = @JoinColumn(name = "owner_id"), name = "token_scope")
  public Set<String> getScope() {
    return scope;
  }

  public void setScope(Set<String> scope) {
    this.scope = scope;
  }

  @Override
  @Transient
  public boolean isExpired() {
    return getExpiration() == null ? false : System.currentTimeMillis() > getExpiration().getTime();
  }

  /**
   * @return the jwtValue
   */
  @Basic
  @Column(name = "token_value")
  @Convert(converter = JWTStringConverter.class)
  public JWT getJwt() {
    return jwtValue;
  }

  /**
   * @param jwtValue the jwtValue to set
   */
  public void setJwt(JWT jwt) {
    this.jwtValue = jwt;
  }

  /**
   * @return the tokenValueHash
   */
  @Basic
  @Column(name = "token_value_hash", length = 64)
  public String getTokenValueHash() {
    return tokenValueHash;
  }

  public void setTokenValueHash(String hash) {
    this.tokenValueHash = hash;
  }

  @Override
  @Transient
  public int getExpiresIn() {

    if (getExpiration() == null) {
      return -1; // no expiration time
    } else {
      int secondsRemaining =
          (int) ((getExpiration().getTime() - System.currentTimeMillis()) / 1000);
      if (isExpired()) {
        return 0; // has an expiration time and expired
      } else { // has an expiration time and not expired
        return secondsRemaining;
      }
    }
  }

  /**
   * @return the permissions
   */
  @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  @JoinTable(name = "access_token_permissions", joinColumns = @JoinColumn(name = "access_token_id"),
      inverseJoinColumns = @JoinColumn(name = "permission_id"))
  public Set<Permission> getPermissions() {
    return permissions;
  }

  /**
   * @param permissions the permissions to set
   */
  public void setPermissions(Set<Permission> permissions) {
    this.permissions = permissions;
  }

  @ManyToOne
  @JoinColumn(name = "approved_site_id")
  public ApprovedSite getApprovedSite() {
    return approvedSite;
  }

  public void setApprovedSite(ApprovedSite approvedSite) {
    this.approvedSite = approvedSite;
  }

  /**
   * Add the ID Token to the additionalInformation map for a token response.
   * 
   * @param idToken
   */
  @Transient
  public void setIdToken(JWT idToken) {
    if (idToken != null) {
      additionalInformation.put(ID_TOKEN_FIELD_NAME, idToken.serialize());
    }
  }

  @Transient
  public Set<String> getAudiences() {
    try {
      return jwtValue.getJWTClaimsSet().getAudience().stream().collect(Collectors.toSet());
    } catch (ParseException e) {
      return Set.of();
    }
  }

  public void hashMe() {
    if (jwtValue != null) {
      this.tokenValueHash =
          Hashing.sha256().hashString(jwtValue.serialize(), StandardCharsets.UTF_8).toString();
    }
  }
}
