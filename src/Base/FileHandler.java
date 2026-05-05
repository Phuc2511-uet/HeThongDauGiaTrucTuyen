package Base;

import java.io.*;

public class FileHandler {
    // luu objects vao file
    public static void save(Object obj, String fileName) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(obj);
        }
    }

    // doc data tu file
    public static Object load(String fileName) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
            return ois.readObject();
        }
    }
}