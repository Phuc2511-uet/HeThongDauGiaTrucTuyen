package NetWork;

import AuctionManager.AuctionManager;
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

                default:
                    return "ERROR Unknown action";
            }

        } catch (Exception e){
            return "ERROR " + e.getMessage();
        }
    }

    // ===== PLACE BID =====
    // FORMAT: PLACE_BID auctionId price(user co san)
    private String handlePlaceBid(String[] parts, User user) {

        int auctionId = Integer.parseInt(parts[1]);
        double price = Double.parseDouble(parts[2]);

        Bidder bidder = (Bidder) user;

        AuctionManager.getInstance()
                .placeBid(auctionId, bidder, price);

        return "BID_SUCCESS";
    }
}