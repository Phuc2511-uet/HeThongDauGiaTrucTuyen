package Controllers.NetWork;

import Controllers.Exceptions.AuctionClosedException;
import Controllers.Exceptions.InvalidBidException;
import Model.Auction.Auction;
import Model.AuctionManager.AuctionManager;
import Model.Item.Item;
import Model.Item.ItemManager;
import Model.User.*;

import java.util.List;

public class InformationHandle {

    private static volatile InformationHandle instance;

    private InformationHandle() {}

    public static InformationHandle getInstance() {
        if (instance == null) {
            synchronized (InformationHandle.class) {
                if (instance == null) {
                    instance = new InformationHandle();
                }
            }
        }
        return instance;
    }

    // ===== MAIN HANDLE =====
    public String handleIfo(String s, User currentUser){
        try {
            String[] part = s.trim().split("\\s+");

            if (part.length == 0) {
                return "ERROR Empty request";
            }

            String action = part[0];

            switch (action){
                case "PLACE_BID":
                    return handlePlaceBid(part,currentUser);
                case "CREATE_AUCTION":
                    return handleCreateAuction(part,currentUser);
                case "GET_AUCTIONS":
                    return handleGetAuction();
                case "NEW_ACCOUNT":
                    return handleNewAccount(part);
                case "GET_AUCTION_BY_ID":
                    return handleGetAuctionById(part);
                case "UPDATE_ITEM_PRICE":
                    return handleUpdateItemPrice(part, currentUser);
                case "DELETE_ITEM":
                    return handleDeleteItem(part, currentUser);
                case "CREATE_ITEM":
                    return handleCreateItem(part, currentUser);
                case "GET_USER_BY_ID":
                    return handleGetUserById(part);//thg san tu lam trong client
                case "DELETE_USER":
                    return handleDeleteUser(part, currentUser);
                case "GET_USER_IDS":
                    return handleGetUserIds();
                case "GET_ITEM_BY_ID":
                    return handleGetItemById(part);
                case "GET_ITEM_IDS":
                    return handleGetItemIds();
                case "DEPOSIT":
                    return handleDeposit(part, currentUser);
                case "PAY":
                    return handlePay(part, currentUser);
                case "GET_WON_AUCTIONS":
                    return handleGetWonAuctions(currentUser);
                case "GET_SELLER_AUCTIONS":
                    return handleGetSellerAuctions(currentUser);
                case "GET_CURRENT_USER":
                    return handleGetCurrentUser(currentUser);
                case "AUTO_BID":
                    return handleAutoBid(part, currentUser);

                default:
                    return "ERROR Unknown action";
            }

        } catch (Exception e){
            return "ERROR " + e.getMessage();
        }
    }




    private String handleGetSellerAuctions(User currentUser) {

        try {
            if (!(currentUser instanceof Seller)) {
                return "ERROR ONLY SELLER";
            }

            Seller seller = (Seller) currentUser;

            List<Auction> auctions = AuctionManager.getInstance().getAllAuctions();

            StringBuilder sb = new StringBuilder();
            sb.append("SELLER_AUCTIONS");

            for (Auction a : auctions) {

                if (seller.equals(a.getSeller())) {
                    sb.append(" ").append(a.getId());
                }
            }

            return sb.toString();

        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }


    private String handleGetWonAuctions(User currentUser) {

        try {
            if (!(currentUser instanceof Bidder)) {
                return "ERROR ONLY BIDDER";
            }

            Bidder bidder = (Bidder) currentUser;

            List<Auction> auctions = AuctionManager.getInstance().getAllAuctions();

            StringBuilder sb = new StringBuilder();
            sb.append("WON_AUCTIONS");

            for (Auction a : auctions) {

                if ((a.getStatus() == Auction.Status.FINISH
                        || a.getStatus() == Auction.Status.PAID)
                        && bidder.equals(a.getCurrentBidder())) {

                    sb.append(" ").append(a.getId());
                }
            }

            return sb.toString();

        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }


    private String handlePay(String[] parts, User currentUser) {

        try {
            if (parts.length < 2) {
                return "ERROR INVALID FORMAT";
            }

            int auctionId = Integer.parseInt(parts[1]);

            Auction auction = AuctionManager.getInstance()
                    .getAuctionById(auctionId);

            if (auction == null) {
                return "ERROR AUCTION NOT FOUND";
            }

            if (!(currentUser instanceof Bidder)) {
                return "ERROR ONLY BIDDER CAN PAY";
            }

            //  check người thắng
            if (!currentUser.equals(auction.getCurrentBidder())) {
                return "ERROR NOT WINNER";
            }

            //  gọi pay (boolean)
            boolean ok = auction.pay();

            if (!ok) {
                return "PAY_FAILED";
            }

            return "PAY_SUCCESS " + auctionId;

        } catch (Exception e) {
            return "PAY_FAILED " + e.getMessage();
        }
    }







    private String handleGetAuction() {
        try {
            return AuctionManager.getInstance().getAuction();
        } catch (Exception e) {
            return "ERROR GET_AUCTION " + e.getMessage();
        }
    }
    private String handleDeposit(String[] parts, User currentUser) {

        try {
            if (!(currentUser instanceof Bidder)) {
                return "ERROR ONLY BIDDER CAN DEPOSIT";
            }

            double amount = Double.parseDouble(parts[1]);

            Bidder bidder = (Bidder) currentUser;

            boolean ok = bidder.deposit(amount);

            if (!ok) {
                return "DEPOSIT_FAILED";
            }

            return "DEPOSIT_SUCCESS";

        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }
    private String handleGetItemIds() {

        try {
            return ItemManager.getInstance().getAllItemIdsAsString();
        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }
    private String handleGetItemById(String[] parts) {

        try {
            if (parts.length < 2) {
                return "ERROR INVALID FORMAT";
            }

            int id = Integer.parseInt(parts[1]);

            return ItemManager.getInstance().getItemInfoAsString(id);

        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }


    private String handleGetUserIds() {
        try {
            return UserManager.getInstance().getAllUserIdsAsString();
        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }
    private String handleGetUserById(String[] parts) {

        try {
            if (parts.length < 2) {
                return "ERROR INVALID FORMAT";
            }

            int userId = Integer.parseInt(parts[1]);

            return UserManager.getInstance()
                    .getUserInfoAsString(userId);

        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }

    private String handleGetCurrentUser(User currentUser) {

        try {
            if (currentUser == null) {
                return "ERROR NOT LOGIN";
            }

            String role = "UNKNOWN";
            double balance = -1; // mặc định không có

            if (currentUser instanceof Bidder) {
                role = "BIDDER";
                balance = ((Bidder) currentUser).getBalance();
            }
            else if (currentUser instanceof Seller) {
                role = "SELLER";
                balance = ((Seller) currentUser).getBalance();
            }
            else if (currentUser instanceof Admin) {
                role = "ADMIN";
            }

            String base = "USER_DETAIL "
                    + currentUser.getId() + " "
                    + currentUser.getUsername() + " "
                    + role + " "
                    + currentUser.getFullName().replace(" ", "_");

            // chỉ thêm balance nếu có
            if (balance >= 0) {
                base += " " + balance;
            }

            return base;

        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }


    private String handleDeleteUser(String[] parts, User currentUser) {

        try {
            if (parts.length < 2) {
                return "ERROR INVALID FORMAT";
            }

            int userId = Integer.parseInt(parts[1]);

            // (khuyên dùng) phân quyền
            if (!(currentUser instanceof Admin)) {
                return "ERROR ONLY ADMIN CAN DELETE USER";
            }

            // không cho tự xoá chính mình
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

    // ===== PLACE BID =====
    // FORMAT: PLACE_BID auctionId price(user co san)
    private String handlePlaceBid(String[] parts, User user) {

        try {
            int auctionId = Integer.parseInt(parts[1]);
            double price = Double.parseDouble(parts[2]);

            Bidder bidder = (Bidder) user;

            AuctionManager.getInstance()
                    .placeBid(auctionId, bidder, price);

            return "BID_SUCCESS";

        } catch (AuctionClosedException e) {
            return "BID_FAILED " + e.getMessage();
        } catch (InvalidBidException e) {
            return "BID_FAILED " + e.getMessage();
        }

    }
    private String handleUpdateItemPrice(String[] parts, User currentUser) {  //UPDATE_ITEM_PRICE itemId newPrice

        try {
            if (parts.length < 3) {
                return "ERROR INVALID FORMAT";
            }

            int itemId = Integer.parseInt(parts[1]);
            double newPrice = Double.parseDouble(parts[2]);

            if (!(currentUser instanceof Seller)) {
                return "ERROR ONLY SELLER CAN UPDATE ITEM";
            }

            boolean ok = ItemManager.getInstance()
                    .updatePrice(itemId, newPrice);

            if (!ok) {
                return "UPDATE_PRICE_FAILED";
            }

            return "UPDATE_PRICE_SUCCESS";

        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }
    private String handleCreateAuction(String[] parts, User currentUser) {

        try {
            int itemId = Integer.parseInt(parts[1]);
            double startPrice = Double.parseDouble(parts[3]);

            Seller seller = (Seller) currentUser;

            AuctionManager.getInstance()
                    .newAuction(itemId, seller, startPrice);

            return "CREATE_AUCTION_SUCCESS";

        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }
    private String handleDeleteItem(String[] parts, User currentUser) {  //DELETE_ITEM itemId

        try {
            if (parts.length < 2) {
                return "DELETE_ITEM_FAILED";
            }

            int itemId = Integer.parseInt(parts[1]);

            if (!(currentUser instanceof Admin)) {
                return "DELETE_ITEM_FAILED";
            }

            ItemManager.getInstance().remove(itemId);

            return "DELETE_ITEM_SUCCESS";

        } catch (Exception e) {
            return "DELETE_ITEM_FAILED";
        }
    }
    private String handleNewAccount(String[] parts) {

        try {
            if (parts.length < 5) {
                return "ERROR INVALID NEW_ACCOUNT FORMAT";
            }

            String username = parts[1];
            String password = parts[2];
            String role = parts[3];
            String fullName = parts[4].replace("_", " ");

            UserManager um = UserManager.getInstance();

            //  check username trùng
            for (User u : um.getUsers()) {
                if (u.getUsername().equals(username)) {
                    return "ACCOUNT_FAILED USERNAME_EXISTS";
                }
            }

            // tạo user (auto id bên trong)
            um.createUser(username, password, role, fullName);

            return "ACCOUNT_SUCCESS";

        } catch (IllegalArgumentException e) {
            return "ACCOUNT_FAILED " ;
        } catch (Exception e) {
            return "ACCOUNT_FAILED";
        }
    }
    private String handleGetAuctionById(String[] parts) {

        try {
            int id = Integer.parseInt(parts[1]);

            Auction a = AuctionManager.getInstance().getAuctionById(id);

            if (a == null) {
                return "ERROR Auction not found";
            }
            return "AUCTION_DETAIL_SUCCESS "
                    + a.getId() + " "
                    + a.getItem().getName().replace(" ", "_") + " "
                    + a.getCurrentPrice() + " "
                    + a.getSeller().getUsername() + " "
                    + a.getStatus()+ " "
                    + (a.getCurrentBidder() != null ? a.getCurrentBidder().getUsername() : "NONE");
        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }
    private String handleCreateItem(String[] parts, User currentUser) {

        try {
            if (parts.length < 4) {
                return "ERROR INVALID FORMAT";
            }

            String type = parts[1];
            String name = parts[2].replace("_", " ");
            double price = Double.parseDouble(parts[3]);

            if (!(currentUser instanceof Seller)) {
                return "ERROR ONLY SELLER CAN CREATE ITEM";
            }

            // Ép kiểu currentUser thành Seller
            Seller seller = (Seller) currentUser;

            //  tạo qua ItemManager, truyền thêm seller
            Item item = ItemManager.getInstance()
                    .createItem(type, name, price, seller); // Truyền seller vào đây

            return "CREATE_ITEM_SUCCESS " ;

        } catch (IllegalArgumentException e) {
            return "ERROR " + e.getMessage();
        } catch (Exception e) {
            return "ERROR CREATE_ITEM_FAILED";
        }
    }private String handleAutoBid(String[] parts, User user) {

        try {
            if (!(user instanceof Bidder)) {
                return "AUTO_BID_FAILED";
            }

            int auctionId = Integer.parseInt(parts[1]);
            double maxPrice = Double.parseDouble(parts[2]);

            Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);

            if (auction == null) {
                return " Auction_không_tồn_tại";
            }

            auction.registerAutoBid((Bidder) user, maxPrice);

            return "AUTO_BID_SUCCESS";

        } catch (Exception e) {
            return "AUTO_BID_FAILED " + e.getMessage();
        }
    }


}