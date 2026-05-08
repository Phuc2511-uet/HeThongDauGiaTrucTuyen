package NetWork;

import Auction.Auction;
import AuctionManager.AuctionManager;
import Factory.*;
import Item.*;
import User.*;

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
                case "GET_AUCTION":
                    return handleGetAuction();
                case "NEW_ACCOUNT":
                    return handleNewAccount(part);
                case "GET_AUCTION_BY_ID":
                    return handleGetAuctionById(part);// thg san tu lam trong client cai nay
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
                default:
                    return "ERROR Unknown action";
            }

        } catch (Exception e){
            return "ERROR " + e.getMessage();
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

        } catch (Exception e) {
            return "ERROR " + e.getMessage();
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
                return "ERROR ITEM NOT FOUND OR UPDATE FAILED";
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
                return "ERROR INVALID FORMAT";
            }

            int itemId = Integer.parseInt(parts[1]);

            if (!(currentUser instanceof Seller)) {
                return "ERROR ONLY SELLER CAN DELETE ITEM";
            }

            ItemManager.getInstance().remove(itemId);

            return "DELETE_ITEM_SUCCESS";

        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }
    private String handleNewAccount(String[] parts) {

        try {
            // kiểm tra đủ tham số
            if (parts.length < 5) {
                return "ERROR INVALID NEW_ACCOUNT FORMAT";
            }

            String username = parts[1];
            String password = parts[2];
            String role = parts[3];
            String fullName = parts[4].replace("_", " "); // nếu client có thay space

            User user;

            switch (role) {

                case "BIDDER":
                    user = new Bidder(username, password, fullName);
                    break;

                case "SELLER":
                    user = new Seller(username, password, fullName);
                    break;

                default:
                    return "ACCOUNT_FAILED INVALID_ROLE";
            }

            boolean success = UserManager.getInstance().addUser(user);

            if (!success) {
                return "ACCOUNT_FAILED";
            }

            return "ACCOUNT_SUCCESS";

        } catch (Exception e) {
            return "ACCOUNT_FAILED ";
        }
    }
    private String handleGetAuctionById(String[] parts) {

        try {
            int id = Integer.parseInt(parts[1]);

            Auction a = AuctionManager.getInstance().getAuctionById(id);

            if (a == null) {
                return "ERROR Auction not found";
            }

            // format: AUCTION_DETAIL id name price seller status
            return "AUCTION_DETAIL "
                    + a.getId() + " "
                    + a.getItem().getName().replace(" ", "_") + " "
                    + a.getCurrentPrice() + " "
                    + a.getSeller().getUsername() + " "
                    + a.getStatus();

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

            ItemFactory factory;

            switch (type) {

                case "ELECTRONIC":
                    factory = new ElectronicCreator();
                    break;

                case "VEHICLE":
                    factory = new VehicleCreator();
                    break;

                case "ART":
                    factory = new ArtCreator();
                    break;

                default:
                    return "ERROR UNKNOWN ITEM TYPE";
            }

            Item item = factory.CreateItem(name, price);

            ItemManager.getInstance().addItem(item);

            return "CREATE_ITEM_SUCCESS " + item.getId();

        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }
}