package skysb.Utils;

import org.bukkit.util.Vector;

public class VectorUtils {
    public static String VectorToString(Vector v)
    {
        return "" + v.getX() + "|" + v.getY() + "|" +v.getZ();
    }

    public static Vector StringToVector(String s)
    {
        String[] coords = s.split("[|]");

        return new Vector(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]), Double.parseDouble(coords[2]));
    }
}
