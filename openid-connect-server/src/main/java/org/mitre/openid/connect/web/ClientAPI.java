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
package org.mitre.openid.connect.web;

import java.util.Collection;

import org.mitre.jwt.assertion.AssertionValidator;
import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.service.ClientDetailsEntityService;
import org.mitre.oauth2.web.AuthenticationUtilities;
import org.mitre.openid.connect.model.CachedImage;
import org.mitre.openid.connect.service.ClientLogoLoadingService;
import org.mitre.openid.connect.view.ClientEntityViewForAdmins;
import org.mitre.openid.connect.view.ClientEntityViewForUsers;
import org.mitre.openid.connect.view.HttpCodeView;
import org.mitre.openid.connect.view.JsonEntityView;
import org.mitre.openid.connect.view.JsonErrorView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.common.base.Strings;

/**
 * @author Michael Jett <mjett@mitre.org>
 */

@Controller
@RequestMapping("/" + ClientAPI.URL)
@PreAuthorize("hasRole('ROLE_USER')")
public class ClientAPI {

  public static final String URL = RootController.API_URL + "/clients";

  @Autowired
  private ClientDetailsEntityService clientService;

  @Autowired
  private ClientLogoLoadingService clientLogoLoadingService;

  @Autowired
  @Qualifier("clientAssertionValidator")
  private AssertionValidator assertionValidator;

  /**
   * Logger for this class
   */
  private static final Logger logger = LoggerFactory.getLogger(ClientAPI.class);

  /**
   * Get a list of all clients
   * 
   * @param modelAndView
   * @return
   */
  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public String apiGetAllClients(Model model, Authentication auth) {

    Collection<ClientDetailsEntity> clients = clientService.getAllClients();
    model.addAttribute(JsonEntityView.ENTITY, clients);

    if (AuthenticationUtilities.isAdmin(auth)) {
      return ClientEntityViewForAdmins.VIEWNAME;
    }
    return ClientEntityViewForUsers.VIEWNAME;
  }

  /**
   * Get an individual client
   * 
   * @param id
   * @param modelAndView
   * @return
   */
  @RequestMapping(value = "/{id}", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public String apiShowClient(@PathVariable("id") Long id, Model model, Authentication auth) {

    ClientDetailsEntity client = clientService.getClientById(id);

    if (client == null) {
      logger.error("apiShowClient failed; client with id " + id + " could not be found.");
      model.addAttribute(HttpCodeView.CODE, HttpStatus.NOT_FOUND);
      model.addAttribute(JsonErrorView.ERROR_MESSAGE,
          "The requested client with id " + id + " could not be found.");
      return JsonErrorView.VIEWNAME;
    }

    model.addAttribute(JsonEntityView.ENTITY, client);

    if (AuthenticationUtilities.isAdmin(auth)) {
      return ClientEntityViewForAdmins.VIEWNAME;
    }
    return ClientEntityViewForUsers.VIEWNAME;
  }

  @RequestMapping(value = "/{id}/logo", method = RequestMethod.GET,
      produces = {MediaType.IMAGE_GIF_VALUE, MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
  public ResponseEntity<byte[]> getClientLogo(@PathVariable("id") Long id, Model model) {

    ClientDetailsEntity client = clientService.getClientById(id);

    if (client == null) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    } else if (Strings.isNullOrEmpty(client.getLogoUri())) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    } else {
      // get the image from cache
      CachedImage image = clientLogoLoadingService.getLogo(client);

      if (image == null) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      }

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.parseMediaType(image.getContentType()));
      headers.setContentLength(image.getLength());

      return new ResponseEntity<>(image.getData(), headers, HttpStatus.OK);
    }
  }
}
