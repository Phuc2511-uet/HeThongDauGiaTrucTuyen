package server.service;

import shared.exception.AuctionClosedException;
import shared.exception.InvalidBidException;
import shared.model.auction.Auction;
import shared.model.auction.BidTransaction;
import server.repository.AuctionManager;
import shared.model.user.Admin;
import shared.model.user.Bidder;
import shared.model.user.Seller;
import shared.model.user.User;

import java.util.List;

public class AuctionService {
    public String listAuctions() {
        try {
            return AuctionManager.getInstance().getAuction();
        } catch (Exception e) {
            return "ERROR GET_AUCTION " + e.getMessage();
        }
    }

    public String getAuctionById(String[] parts) {
        try {
            int id = Integer.parseInt(parts[1]);

            // Gọi thẳng hàm lấy trực tiếp từ RAM đã được tối ưu Lock của AuctionManager
            server.repository.AuctionManager manager = server.repository.AuctionManager.getInstance();
            shared.model.auction.Auction auction = manager.getAuctionById(id);

            if (auction == null) {
                return "ERROR Phiên_đấu_giá_không_tồn_tại";
            }

            // Gọi hàm cấu trúc chuỗi mạng chuẩn 9 tham số
            return "AUCTION_DETAIL_SUCCESS " + auction.toNetworkString();

        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }

    public String placeBid(String[] parts, User user) {
        try {
            int auctionId = Integer.parseInt(parts[1]);
            double price = Double.parseDouble(parts[2]);

            AuctionManager.getInstance().placeBid(auctionId, (Bidder) user, price);
            return "BID_SUCCESS";
        } catch (AuctionClosedException e) {
            return "BID_FAILED " + e.getMessage();
        } catch (InvalidBidException e) {
            return "BID_FAILED " + e.getMessage();
        }
    }

    public String createAuction(String[] parts, User currentUser) {
        try {
            int itemId = Integer.parseInt(parts[1]);
            double startPrice = Double.parseDouble(parts[3]);

            AuctionManager.getInstance().newAuction(itemId, (Seller) currentUser, startPrice);
            return "CREATE_AUCTION_SUCCESS";
        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }

    public String pay(String[] parts, User currentUser) {
        try {
            if (parts.length < 2) {
                return "ERROR INVALID FORMAT";
            }

            int auctionId = Integer.parseInt(parts[1]);
            Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);

            if (auction == null) {
                return "ERROR AUCTION NOT FOUND";
            }
            if (!(currentUser instanceof Bidder)) {
                return "ERROR ONLY BIDDER CAN PAY";
            }
            if (!currentUser.equals(auction.getCurrentBidder())) {
                return "ERROR NOT WINNER";
            }

            boolean ok = auction.pay();
            if (!ok) {
                return "PAY_FAILED";
            }

            return "PAY_SUCCESS " + auctionId;
        } catch (Exception e) {
            return "PAY_FAILED " + e.getMessage();
        }
    }

    public String getWonAuctions(User currentUser) {
        try {
            if (!(currentUser instanceof Bidder)) {
                return "ERROR ONLY BIDDER";
            }

            Bidder bidder = (Bidder) currentUser;
            List<Auction> auctions = AuctionManager.getInstance().getAllAuctions();
            StringBuilder response = new StringBuilder("WON_AUCTIONS_LIST");

            for (Auction auction : auctions) {
                if ((auction.getStatus() == Auction.Status.FINISH || auction.getStatus() == Auction.Status.PAID)
                        && bidder.equals(auction.getCurrentBidder())) {
                    response.append(" ")
                            .append(auction.getId()).append("|")
                            .append(auction.getItem().getName().replace(" ", "_")).append("|")
                            .append(auction.getCurrentPrice()).append("|")
                            .append(auction.getStatus().name());
                }
            }

            return response.toString();
        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }

    public String getSellerAuctions(User currentUser) {
        try {
            if (!(currentUser instanceof Seller)) {
                return "ERROR ONLY SELLER";
            }

            Seller seller = (Seller) currentUser;
            StringBuilder response = new StringBuilder("SELLER_AUCTIONS");

            for (Auction auction : AuctionManager.getInstance().getAllAuctions()) {
                if (seller.equals(auction.getSeller())) {
                    response.append(" ")
                            .append(auction.getId()).append("|")
                            .append(auction.getItem().getName().replace(" ", "_"));
                }
            }

            return response.toString();
        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }

    public String registerAutoBid(String[] parts, User user) {
        try {
            if (!(user instanceof Bidder)) {
                return "AUTO_BID_FAILED";
            }

            int auctionId = Integer.parseInt(parts[1]);
            double maxPrice = Double.parseDouble(parts[2]);
            double increment = Double.parseDouble(parts[3]);

            Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);
            if (auction == null) {
                return " Auction_khong_ton_tai";
            }

            auction.registerAutoBid((Bidder) user, maxPrice, increment);
            return "AUTO_BID_SUCCESS " + auctionId + " " + user.getUsername();
        } catch (Exception e) {
            return "AUTO_BID_FAILED " + e.getMessage();
        }
    }

    public String cancelAuction(String[] parts, User currentUser) {
        try {
            if (parts.length < 2 || !(currentUser instanceof Admin)) {
                return "ACTION_FAILED";
            }

            int auctionId = Integer.parseInt(parts[1]);
            Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);
            if (auction != null) {
                auction.cancel();
                System.out.println("Server >> Admin canceled auction ID: " + auctionId);
                return "CANCEL_AUCTION_SUCCESS";
            }
            return "ACTION_FAILED";
        } catch (Exception e) {
            return "ACTION_FAILED " + e.getMessage();
        }
    }

    public String restoreAuction(String[] parts, User currentUser) {
        try {
            if (parts.length < 2 || !(currentUser instanceof Admin)) {
                return "ACTION_FAILED";
            }

            int auctionId = Integer.parseInt(parts[1]);
            Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);
            if (auction != null) {
                auction.resetAuctionTime();
                auction.setStatus(Auction.Status.OPEN);
                System.out.println("Server >> Admin restored auction ID: " + auctionId);
                return "RESTORE_AUCTION_SUCCESS";
            }
            return "ACTION_FAILED";
        } catch (Exception e) {
            return "ACTION_FAILED";
        }
    }

    public String getBidHistory(String[] parts) {
        try {
            if (parts.length < 2) {
                return "ERROR Missing auctionId";
            }

            int auctionId = Integer.parseInt(parts[1]);
            Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);
            if (auction == null) {
                return "ERROR Auction not found";
            }

            StringBuilder response = new StringBuilder("BID_HISTORY ");
            response.append(auctionId);

            for (BidTransaction bid : auction.getBidHistory()) {
                long timeMillis = bid.getBidTime()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();

                response.append(" ")
                        .append(timeMillis)
                        .append(" ")
                        .append(bid.getBidAmount())
                        .append(" ")
                        .append(bid.getBidder().getUsername());
            }

            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR " + e.getMessage();
        }
    }
}
