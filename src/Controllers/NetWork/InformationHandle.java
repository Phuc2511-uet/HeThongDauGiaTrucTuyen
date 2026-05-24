package Controllers.NetWork;

import Controllers.Exceptions.AuctionClosedException;
import Controllers.Exceptions.InvalidBidException;
import Model.Auction.Auction;
import Model.Auction.BidTransaction;
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
                    return handleGetUserById(part);
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
                case "GET_MY_ITEMS":
                    return ItemManager.getInstance().getAvailableItemsBySeller(currentUser.getId());
                case "ADMIN_CREATE_ACCOUNT":
                    return handleAdminCreateAccount(part,currentUser);
                case "SELLER_DELETE_ITEM":
                    return handleSellerDeleteItem(part, currentUser);
                case "CANCEL_AUCTION":
                    return handleCancelAuction(part, currentUser);
                case "RESTORE_AUCTION":
                    return handleRestoreAuction(part, currentUser);
                default:
                    return "ERROR Unknown action";
            }

        } catch (Exception e){
            return "ERROR " + e.getMessage();
        }
    }
    private String handleGetBidHistory(String[] parts) {
        try {
            if (parts.length < 2) {
                return "ERROR Missing auctionId";
            }

            int auctionId = Integer.parseInt(parts[1]);

            Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);

            if (auction == null) {
                return "ERROR Auction not found";
            }

            StringBuilder sb = new StringBuilder("BID_HISTORY ");
            sb.append(auctionId);

            for (BidTransaction b : auction.getBidHistory()) {

                long timeMillis = b.getBidTime()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();

                sb.append(" ")
                        .append(timeMillis)
                        .append(",")
                        .append(b.getBidAmount());
            }

            return sb.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR " + e.getMessage();
        }
    }

    private String handleCancelAuction(String[] parts, User currentUser) {
        try {
            if (parts.length < 2) {
                return "ACTION_FAILED";
            }
            if (!(currentUser instanceof Admin)) {
                return "ACTION_FAILED";
            }
            int auctionId = Integer.parseInt(parts[1]);
            Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);
            if (auction != null) {
                auction.cancel();
                System.out.println("Server >> Admin hủy thành công phiên ID: " + auctionId);
                return "CANCEL_AUCTION_SUCCESS";
            }
            return "ACTION_FAILED";
        } catch (Exception e) {
            return "ACTION_FAILED " + e.getMessage();
        }
    }

    private String handleRestoreAuction(String[] parts, User currentUser) {
        try {
            if (parts.length < 2) {
                return "ACTION_FAILED";
            }
            if (!(currentUser instanceof Model.User.Admin)) {
                return "ACTION_FAILED";
            }
            int auctionId = Integer.parseInt(parts[1]);
            Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);
            if (auction != null) {
                auction.resetAuctionTime();
                auction.setStatus(Auction.Status.OPEN);

                System.out.println("Server >> Admin đã KHÔI PHỤC hoạt động phiên đấu giá ID: " + auctionId);
                return "RESTORE_AUCTION_SUCCESS";
            }
            return "ACTION_FAILED";
        } catch (Exception e) {
            return "ACTION_FAILED";
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
                    sb.append(" ").append(a.getId()).append("|").append(a.getItem().getName().replace(" ","_"));
                }
            }

            return sb.toString();

        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }

    private String handleAdminCreateAccount(String[] parts, User currentUser) {
        try {
            // Chỉ Admin mới có quyền gọi case này
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

            UserManager um = UserManager.getInstance();

            // Kiểm tra trùng lặp tài khoản trên hệ thống
            for (User u : um.getUsers()) {
                if (u.getUsername().equals(username)) {
                    return "ACCOUNT_FAILED USERNAME_EXISTS";
                }
            }

            // Tạo tài khoản mới trực tiếp
            um.createUser(username, password, role, fullName);

            // Trả về từ khóa phản hồi độc lập, tuyệt đối không bị trùng với Client cũ
            return "ADMIN_CREATE_SUCCESS";

        } catch (IllegalArgumentException e) {
            return "ACCOUNT_FAILED";
        } catch (Exception e) {
            return "ACCOUNT_FAILED";
        }
    }


    private String handleGetWonAuctions(User currentUser) {
        try {
            // Chỉ Bidder mới có danh sách
            if (!(currentUser instanceof Bidder)) {
                return "ERROR ONLY BIDDER";
            }

            Bidder bidder = (Bidder) currentUser;
            List<Auction> auctions = AuctionManager.getInstance().getAllAuctions();

            StringBuilder sb = new StringBuilder();
            sb.append("WON_AUCTIONS_LIST");

            for (Auction a : auctions) {
                // Phiên ở trạng thái Finsh hoặc Paid
                // và người giữ giá cao nhất hiện tại chính là người dùng này
                if ((a.getStatus() == Auction.Status.FINISH || a.getStatus() == Auction.Status.PAID) && bidder.equals(a.getCurrentBidder())) {
                    int auctionId = a.getId();
                    String itemName = a.getItem().getName().replace(" ", "_");
                    double winPrice = a.getCurrentPrice();

                    // Định dạng: ID|Tên_Vật_Phẩm|Giá_Thắng|trạng thái
                    sb.append(" ").append(auctionId).append("|").append(itemName).append("|").append(winPrice).append("|").append(a.getStatus().name());
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
                    .getAdminUserInfoAsString(userId);

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
                    + a.getItem().getId() + " "
                    + a.getCurrentPrice() + " "
                    + a.getSeller().getUsername() + " "
                    + a.getStatus().name()+ " "
                    + (a.getCurrentBidder() != null ? a.getCurrentBidder().getUsername() : "NONE");
        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }
    private String handleCreateItem(String[] parts, User currentUser) {
        try {
            // format: CREATE_ITEM TYPE NAME PRICE
            String type = parts[1];
            String name = parts[2];
            double price = Double.parseDouble(parts[3]);

            // Gọi hàm tạo vật phẩm
            ItemManager.getInstance().createItem(type, name, price, (Seller) currentUser);

            return "CREATE_ITEM_SUCCESS";

        } catch (IllegalArgumentException e) {
            return "CREATE_ITEM_FAILED " + e.getMessage();

        } catch (Exception e) {
            return "CREATE_ITEM_FAILED Lỗi_hệ_thống";
        }

    }
    private String handleAutoBid(String[] parts, User user) {

        try {
            if (!(user instanceof Bidder)) {
                return "AUTO_BID_FAILED";
            }

            int auctionId = Integer.parseInt(parts[1]);
            double maxPrice = Double.parseDouble(parts[2]);
            double increament = Double.parseDouble(parts[3]);

            Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);

            if (auction == null) {
                return " Auction_không_tồn_tại";
            }

            auction.registerAutoBid((Bidder) user, maxPrice,increament);

            return "AUTO_BID_SUCCESS";

        } catch (Exception e) {
            return "AUTO_BID_FAILED " + e.getMessage();
        }
    }

    private String handleSellerDeleteItem(String[] parts, User currentUser) {
        try {
            if (parts.length < 2) {
                return "SELLER_DELETE_ITEM_FAILED";
            }
            if (!(currentUser instanceof Seller)) {
                return "SELLER_DELETE_ITEM_FAILED";
            }
            int itemId = Integer.parseInt(parts[1]);
            Item item = ItemManager.getInstance().getById(itemId);

            if (item == null) {
                return "SELLER_DELETE_ITEM_FAILED";
            }

            Seller seller = (Seller) currentUser;

            // chỉ được xóa item của mình
            if (item.getSeller().getId() != seller.getId()) {
                return "SELLER_DELETE_ITEM_FAILED";
            }

            // item đã được đưa lên auction
            // -> không cho xóa
            if (AuctionManager.getInstance().getAuctionByItemId(itemId) != null) {
                return "SELLER_DELETE_ITEM_FAILED";
            }

            ItemManager.getInstance().remove(itemId);

            return "SELLER_DELETE_ITEM_SUCCESS";

        } catch (Exception e) {

            return "SELLER_DELETE_ITEM_FAILED";
        }
    }
}