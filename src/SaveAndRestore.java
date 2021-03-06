import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.*;

class SaveAndRestore {

    private Temp t = new Temp();
    private Connection con = Database.createConnection(t.getA(), t.getB(), t.getC(), t.getD(), t.getE(), "MySQL");

    private String IP = getIP();

    //saving info to database so that the user doesn't have to enter it every time they run the program
    void saveToDB(String conName, String hostName, String port, String databaseName, String user, String pass, String DBtype) {
        try (PreparedStatement prSt = con.prepareStatement("insert into sql_admin values (?,?,?,?,?,?,?,?)")) {
            prSt.setString(1, IP);
            prSt.setString(2, conName);
            prSt.setString(3, hostName);
            prSt.setString(4, port);
            prSt.setString(5, databaseName);
            prSt.setString(6, EncryptDecrypt.encrypt(user));
            prSt.setString(7, EncryptDecrypt.encrypt(pass));
            prSt.setString(8, DBtype);
            prSt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    //automatically restoring info from database
    String restoreFromDB() {
        StringBuilder sb = new StringBuilder();
        try (ResultSet rs = con.createStatement().executeQuery("select * from sql_admin where ip = '" + IP + "'")) {
            while (rs.next()) {
                String conName = rs.getString("connectionName");
                String hostName = rs.getString("hostName");
                String port = rs.getString("port");
                String databaseName = rs.getString("databaseName");
                String user = rs.getString("user");
                String pass = rs.getString("pass");
                String DBtype = rs.getString("DBtype");
                sb.append(conName).append(",").append(hostName).append(",").append(port).append(",").append(databaseName).append(",").append(EncryptDecrypt.decrypt(user)).append(",").append(EncryptDecrypt.decrypt(pass)).append(",").append(DBtype).append(";");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sb.toString().substring(0, sb.toString().length() - 1);
    }


    //removing the info about the user's connection when they press "close connection" button
    void removeFromDB(String conName) {
        try {
            con.createStatement().executeUpdate("delete from mymysqlwb where connectionName = '" + conName + "'");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    //checking if the current computer's IP address is already in the database
    boolean ipInDB() {
        String IP = getIP();
        boolean res = false;

        try (Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("select ip from sql_admin")) {

            while (rs.next()) {
                String ips = rs.getString("ip");
                if (ips.equals(IP)) {
                    res = true;
                    break;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return res;
    }


    //getting the current computers IP address
    private static String getIP() {
        InetAddress IP;
        try {
            IP = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
        return IP.toString();
    }

}
