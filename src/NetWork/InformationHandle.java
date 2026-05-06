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
                    return handleNewAccount(part);//NEW_ACCOUNT username password
                case "GET_AUCTION_BY_ID":
                    return handleGetAuctionById(part);
                case "CREATE_ITEM":
                    return handleCreateItem(part, currentUser);
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