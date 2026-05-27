package server.service;

import shared.model.user.Admin;
import shared.model.user.Bidder;
import shared.model.user.Seller;
import shared.model.user.User;
import server.repository.UserManager;

public class UserService {
    public String getCurrentUser(User currentUser) {
        try {
            if (currentUser == null) {
                return "ERROR NOT LOGIN";
            }

            String role = "UNKNOWN";
            double balance = -1;

            if (currentUser instanceof Bidder) {
                role = "BIDDER";
                balance = ((Bidder) currentUser).getBalance();
            } else if (currentUser instanceof Seller) {
                role = "SELLER";
                balance = ((Seller) currentUser).getBalance();
            } else if (currentUser instanceof Admin) {
                role = "ADMIN";
            }

            String response = "USER_DETAIL "
                    + currentUser.getId() + " "
                    + currentUser.getUsername() + " "
                    + role + " "
                    + currentUser.getFullName().replace(" ", "_");

            if (balance >= 0) {
                response += " " + balance;
            }

            return response;
        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }

    public String deposit(String[] parts, User currentUser) {
        try {
            if (!(currentUser instanceof Bidder)) {
                return "ERROR ONLY BIDDER CAN DEPOSIT";
            }

            double amount = Double.parseDouble(parts[1]);
            if (amount <= 0) {
                return "DEPOSIT_FAILED INVALID_AMOUNT";
            }

            Bidder bidder = (Bidder) currentUser;
            boolean ok = bidder.deposit(amount);

            if (!ok) {
                return "DEPOSIT_FAILED";
            }
            return "DEPOSIT_SUCCESS " + bidder.getBalance();
        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }

    public String getUserIds() {
        try {
            return UserManager.getInstance().getAllUserIdsAsString();
        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }

    public String getUserById(String[] parts) {
        try {
            if (parts.length < 2) {
                return "ERROR INVALID FORMAT";
            }

            int userId = Integer.parseInt(parts[1]);
            return UserManager.getInstance().getAdminUserInfoAsString(userId);
        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }

    public String deleteUser(String[] parts, User currentUser) {
        try {
            if (parts.length < 2) {
                return "ERROR INVALID FORMAT";
            }

            int userId = Integer.parseInt(parts[1]);

            if (!(currentUser instanceof Admin)) {
                return "ERROR ONLY ADMIN CAN DELETE USER";
            }

            if (currentUser.getId() == userId) {
                return "ERROR CANNOT DELETE YOURSELF";
            }

            boolean ok = UserManager.getInstance().removeUser(userId);
            if (!ok) {
                return "ERROR USER NOT FOUND";
            }

            return "DELETE_USER_SUCCESS";
        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }
}
