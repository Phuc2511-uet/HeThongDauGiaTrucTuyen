package User;
public abstract class Entity {
    protected String id;
    //constructor
    public Entity(String id){
        this.id = id;
    }
    //getter and setter
    public String getId(){
        return id;
    }
    public void setId(String id){
        this.id = id;
    }
}
