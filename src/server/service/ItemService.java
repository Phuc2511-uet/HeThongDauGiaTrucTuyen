package server.service;

import server.repository.AuctionManager;
import shared.model.item.Item;
import server.repository.ItemManager;
import shared.model.user.Admin;
import shared.model.user.Seller;
import shared.model.user.User;

public class ItemService {
    public String getMyItems(User currentUser) {
        return ItemManager.getInstance().getAvailableItemsBySeller(currentUser.getId());
    }

    public String getItemIds() {
        try {
            return ItemManager.getInstance().getAllItemIdsAsString();
        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }

    public String getItemById(String[] parts) {
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

    public String createItem(String[] parts, User currentUser) {
        try {
            String type = parts[1];
            String name = parts[2];
            double price = Double.parseDouble(parts[3]);

            StringBuilder imageBuilder = new StringBuilder();

            for (int i = 4; i < parts.length; i++) {
                imageBuilder.append(parts[i]);
            }

            String imageBase64 = imageBuilder.toString();

            ItemManager.getInstance().createItem(type, name, price, imageBase64, (Seller) currentUser);
            return "CREATE_ITEM_SUCCESS";
        } catch (IllegalArgumentException e) {
            return "CREATE_ITEM_FAILED " + e.getMessage();
        } catch (Exception e) {
            return "CREATE_ITEM_FAILED Loi_he_thong";
        }
    }

    public String updateItemPrice(String[] parts, User currentUser) {
        try {
            if (parts.length < 3) {
                return "ERROR INVALID FORMAT";
            }

            int itemId = Integer.parseInt(parts[1]);
            double newPrice = Double.parseDouble(parts[2]);

            if (!(currentUser instanceof Seller)) {
                return "ERROR ONLY SELLER CAN UPDATE ITEM";
            }

            boolean ok = ItemManager.getInstance().updatePrice(itemId, newPrice);
            if (!ok) {
                return "UPDATE_PRICE_FAILED";
            }

            return "UPDATE_PRICE_SUCCESS";
        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }

    public String deleteItemByAdmin(String[] parts, User currentUser) {
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

    public String deleteItemBySeller(String[] parts, User currentUser) {
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
            if (item.getSeller().getId() != seller.getId()) {
                return "SELLER_DELETE_ITEM_FAILED";
            }

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
