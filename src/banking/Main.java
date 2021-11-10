package banking;

import org.sqlite.SQLiteDataSource;

import java.sql.*;
import java.util.Random;
import java.util.Scanner;

public class Main {
    public static Scanner scanner = new Scanner(System.in);
    static SQLiteDataSource dataSource = new SQLiteDataSource();

    public static void main(String[] args) {
        String dbName = "card.s3db";
        String url = "jdbc:sqlite:" + dbName;

        dataSource.setUrl(url);

        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {
                Statement statement = connection.createStatement();
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS card (" + "id INTEGER," + "number TEXT," + "pin TEXT," + "balance INTEGER DEFAULT 0);");
                while (true) {
                    printMenu();
                    int cmd = scanner.nextInt();
                    if (cmd == 0) {
                        System.out.println("Bye!");
                        System.exit(0);
                    } else if (cmd == 1) {
                        createAccount(statement);
                    } else if (cmd == 2) {
                        checkInfo(statement);
                    }
                }
            }
        } catch (SQLException e) {
                e.printStackTrace();
        }
    }

    public static void printMenu() {
        System.out.println("1. Create an account");
        System.out.println("2. Log into account");
        System.out.println("0. Exit");
    }

    public static void createAccount(Statement stmt) throws SQLException {
        String cardNumber = "400000";
        Random random = new Random();
        int randomNumber = random.nextInt(89999) + 10000;
        cardNumber = String.format("%s%09d", cardNumber, randomNumber);
        int checkSum = findCheckSum(cardNumber);
        cardNumber = String.format("%s%d", cardNumber, checkSum);
        int pin = randomNumber % 10000;
        String passwordValue = String.format("%04d", pin);
        stmt.executeUpdate("INSERT INTO card(number,pin) VALUES('" + cardNumber + "', '" + passwordValue + "');");
        System.out.println("Your card number:\n" + cardNumber);
        System.out.println("Your card PIN:\n" + passwordValue);
    }

    private static int findCheckSum(String cardNumber) {
        int ans = 0;
        for (int i = 0; i < cardNumber.length(); i++) {
            int temp = cardNumber.charAt(i) - 48;
            if (i%2==0) {
                temp *= 2;
            }
            if (temp > 9) {
                temp -= 9;
            }
            ans += temp;
        }
        int fina = 10 - (ans % 10);
        fina = fina == 10 ? 0 : fina;
        return fina;
    }

    public static void checkInfo(Statement statement) throws SQLException {
        System.out.println("Enter your card number:");
        scanner.nextLine();
        String card = scanner.nextLine();
        System.out.println("Enter your PIN:");
        String pin = scanner.nextLine();
        ResultSet cardFinder = statement.executeQuery("SELECT * FROM card WHERE number =" + card + " AND pin =" + pin);
        if (cardFinder.next()) {
            System.out.println("You have successfully logged in!");
            while (true) {
                printInMenu();
                int val = scanner.nextInt();
                if (val == 0) {
                    System.out.println("Bye!");
                    System.exit(0);
                } else if (val == 1) {
                    System.out.println("Balance: " + cardFinder.getInt("balance"));
                } else if (val == 2) {
                    addMoney(statement, card);
                } else if (val == 3) {
                    transfer(statement, card);
                } else if (val == 4) {
                    closeAccount(statement, card);
                } else if (val == 5) {
                    System.out.println("You have successfully logged out!");
                    break;
                }
            }
        }
    }

    private static void transfer(Statement statement, String card) throws SQLException {
        System.out.println("Transfer\n" + "Enter card number:");
        String toCardNumber = scanner.next();
        int checkSum = findCheckSum(toCardNumber.substring(0, 15));
        if (checkSum != Integer.parseInt(String.valueOf(toCardNumber.charAt(15)))) {
            System.out.println("Probably you made a mistake in the card number. Please try again!");
        } else {
            ResultSet fromCard = statement.executeQuery("SELECT * FROM card WHERE number = " + card);
            int currentBalance = fromCard.getInt("balance");
            if (!accountExists(toCardNumber)) {
                System.out.println("Such a card does not exist.");
            } else {
                System.out.println("Enter how much money you want to transfer:");
                int transferAmt = scanner.nextInt();
                if (transferAmt > currentBalance) {
                    System.out.println("Not enough money!");
                } else {
                    statement.executeUpdate("UPDATE card SET balance = balance + " + transferAmt + " WHERE number = " + toCardNumber);
                    statement.executeUpdate("UPDATE card SET balance = balance - " + transferAmt + " WHERE number = " + card);
                    System.out.println("Success!");
                }
            }
        }
    }

    public static boolean accountExists(String number) {
        try (Connection con = dataSource.getConnection()) {

            String select = "SELECT * FROM card WHERE number = ?";

            try (PreparedStatement preparedStatement = con.prepareStatement(select)) {

                preparedStatement.setString(1, number);

                // process result
                ResultSet resultSet = preparedStatement.executeQuery();

                // check if ResultSet is not empty
                if (resultSet.next()) {
                    return true;
                } else {
                    return false;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void addMoney(Statement statement, String card) throws SQLException {
        System.out.println("Enter income:");
        int amount = scanner.nextInt();
        statement.executeUpdate("UPDATE card SET balance = balance + " + amount + " WHERE number =" + card);
        System.out.println("Income was added!");
    }

    private static void closeAccount(Statement statement, String card) throws SQLException {
        statement.executeUpdate("DELETE FROM card WHERE number =" + card);
    }

    private static void printInMenu() {
        System.out.println("1. Balance");
        System.out.println("2. Add income");
        System.out.println("3. Do transfer");
        System.out.println("4. Close account");
        System.out.println("5. Log out");
        System.out.println("0. Exit");
    }
}