package server.service;

import shared.model.user.Admin;
import shared.model.user.User;
import server.repository.UserManager;

public class AccountService {
    public String createPublicAccount(String[] parts) {
        try {
            if (parts.length < 5) {
                return "ERROR INVALID NEW_ACCOUNT FORMAT";
            }

            String username = parts[1];
            String password = parts[2];
            String role = parts[3];
            String fullName = parts[4].replace("_", " ");

            UserManager userManager = UserManager.getInstance();
            for (User user : userManager.getUsers()) {
                if (user.getUsername().equals(username)) {
                    return "ACCOUNT_FAILED USERNAME_EXISTS";
                }
            }

            userManager.createUser(username, password, role, fullName);
            return "ACCOUNT_SUCCESS";
        } catch (IllegalArgumentException e) {
            return "ACCOUNT_FAILED ";
        } catch (Exception e) {
            return "ACCOUNT_FAILED";
        }
    }

    public String createAccountByAdmin(String[] parts, User currentUser) {
        try {
            if (!(currentUser instanceof Admin)) {
                return "ACCOUNT_FAILED NOT_AUTHORIZED";
            }

            if (parts.length < 5) {
                return "ACCOUNT_FAILED INVALID_FORMAT";
            }

            String username = parts[1];
            String password = parts[2];
            String role = parts[3];
            String fullName = parts[4].replace("_", " ");

            UserManager userManager = UserManager.getInstance();
            for (User user : userManager.getUsers()) {
                if (user.getUsername().equals(username)) {
                    return "ACCOUNT_FAILED USERNAME_EXISTS";
                }
            }

            userManager.createUser(username, password, role, fullName);
            return "ADMIN_CREATE_SUCCESS";
        } catch (Exception e) {
            return "ACCOUNT_FAILED";
        }
    }
}
