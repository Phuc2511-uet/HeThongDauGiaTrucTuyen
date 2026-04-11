public abstract class Entity {
    protected String idItem;
    //constructor
    public Entity(String idItem){
        this.idItem = idItem;
    }
    //getter and setter
    public String getId(){
        return idItem;
    }
    public void setId(String idItem){
        this.idItem = idItem;
    }
}
