package App;
import Factory.*;
import Item.*;

public class Main {
    public static void main(String args[]){
        ItemFactory carFactory = new VehicleCreator();
        Item car = carFactory.CreateItem("001","toyota",21.34);
        car.display();
    }
}
