package com.ionista.service;

import com.ionista.entity.User;

public interface OAuthUserService {

    User findOrCreateGoogleUser(String email, String providerId, String firstName, String lastName, boolean emailVerified);
}
