package com.ionista.service;

import com.ionista.entity.User;

public interface RefreshTokenService {

    String issueRefreshToken(User user);

    User validateAndConsume(String rawToken);

    void revoke(String rawToken);
}
