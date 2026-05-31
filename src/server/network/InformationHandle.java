package server.network;

import server.service.AccountService;
import server.service.AuctionService;
import server.service.ItemService;
import server.service.UserService;
import shared.model.user.User;

public class InformationHandle {
    private static volatile InformationHandle instance;

    private final AccountService accountService = new AccountService();
    private final AuctionService auctionService = new AuctionService();
    private final ItemService itemService = new ItemService();
    private final UserService userService = new UserService();

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

    public String handleIfo(String request, User currentUser) {
        try {
            String[] parts = request.trim().split("\\s+");
            if (parts.length == 0) {
                return "ERROR Empty request";
            }

            String action = parts[0];

            switch (action) {
                case "NEW_ACCOUNT":
                    return accountService.createPublicAccount(parts);
                case "ADMIN_CREATE_ACCOUNT":
                    return accountService.createAccountByAdmin(parts, currentUser);

                case "GET_CURRENT_USER":
                    return userService.getCurrentUser(currentUser);
                case "DEPOSIT":
                    return userService.deposit(parts, currentUser);
                case "GET_USER_IDS":
                    return userService.getUserIds();
                case "GET_USER_BY_ID":
                    return userService.getUserById(parts);
                case "DELETE_USER":
                    return userService.deleteUser(parts, currentUser);

                case "GET_MY_ITEMS":
                    return itemService.getMyItems(currentUser);
                case "GET_ITEM_IDS":
                    return itemService.getItemIds();
                case "GET_ITEM_BY_ID":
                    return itemService.getItemById(parts);
                case "CREATE_ITEM":
                    return itemService.createItem(parts, currentUser);
                case "UPDATE_ITEM_PRICE":
                    return itemService.updateItemPrice(parts, currentUser);
                case "DELETE_ITEM":
                    return itemService.deleteItemByAdmin(parts, currentUser);
                case "SELLER_DELETE_ITEM":
                    return itemService.deleteItemBySeller(parts, currentUser);

                case "GET_AUCTIONS":
                    return auctionService.listAuctions();
                case "GET_AUCTION_BY_ID":
                    return auctionService.getAuctionById(parts);
                case "CREATE_AUCTION":
                    return auctionService.createAuction(parts, currentUser);
                case "PLACE_BID":
                    return auctionService.placeBid(parts, currentUser);
                case "PAY":
                    return auctionService.pay(parts, currentUser);
                case "GET_WON_AUCTIONS":
                    return auctionService.getWonAuctions(currentUser);
                case "GET_SELLER_AUCTIONS":
                    return auctionService.getSellerAuctions(currentUser);
                case "AUTO_BID":
                    return auctionService.registerAutoBid(parts, currentUser);
                case "CANCEL_AUCTION":
                    return auctionService.cancelAuction(parts, currentUser);
                case "RESTORE_AUCTION":
                    return auctionService.restoreAuction(parts, currentUser);
                case "GET_BID_HISTORY":
                    return auctionService.getBidHistory(parts);
                case "LEAVE_AUCTION":
                    return "LEAVE_AUCTION_SUCCESS";
                default:
                    return "ERROR Unknown action";
            }
        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }
}
