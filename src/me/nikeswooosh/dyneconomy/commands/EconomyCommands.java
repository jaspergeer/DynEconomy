package me.nikeswooosh.dyneconomy.commands;

import me.nikeswooosh.dyneconomy.DynEconomy;
import me.nikeswooosh.dyneconomy.utils.Utils;
import net.tnemc.core.economy.Account;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class EconomyCommands implements CommandExecutor {

    private final DynEconomy plugin;
    private final Material currency;

    /**
     * Creates a new EconomyCommands instance from the plugin config.
     *
     * @param plugin DynEconomy instance these commands are associated with
     */
    public EconomyCommands(DynEconomy plugin) {
        this.plugin = plugin;
        currency = Material.matchMaterial(Objects.requireNonNull(plugin.getConfig().getString("currency_item")));
        Objects.requireNonNull(plugin.getCommand("dyneconomy")).setExecutor(this);
    }

    // getter commands

    /**
     * Displays the current total money in circulation, money supply limit, and
     * currency item unit price in a players chat box.
     * Sub-command of /dyneconomy.
     *
     * @param p Player to broadcast message to
     */
    void infoCommand(Player p) {
        p.sendMessage("");
        p.sendMessage("");
        p.sendMessage("");
        p.sendMessage("");
        p.sendMessage("");
        p.sendMessage("");
        p.sendMessage(Utils.chat("&2Dynamic Economy Overview"));
        p.sendMessage(Utils.chat("&6==========================="));
        p.sendMessage(Utils.chat("&2Curent Money Supply: &e" + plugin.getTotalMoney()));
        p.sendMessage(Utils.chat("&2Money Supply Limit: &e" + plugin.getMaxMoney()));
        p.sendMessage(Utils.chat("&2Unit Price of &5" + currency.name() +"&2: &e" + plugin.getUnitPrice()));
        p.sendMessage(Utils.chat("&6==========================="));
    }

    // setter/interaction commands

    /**
     * Player sells a given amount of currency item to the server and recieives
     * compensation accordingly. The per-unit price is calculated for each item
     * so selling differing quantities will not affect the compensation amount.
     * Sub-command of /dyneconomy.
     *
     * @param p Player selling items to the server
     */
    void sellCommand(Player p) {
        ItemStack handStack = p.getInventory().getItemInMainHand();
        if (handStack.getType().equals(currency)) {
            int num_sold = handStack.getAmount();
            handStack.setAmount(0);
            BigDecimal before = plugin.getEcoAPI().getAccount(p.getUniqueId()).getHoldings();
            for (int i = 0; i < num_sold; i++) {
                BigDecimal s = BigDecimal.valueOf(plugin.getUnitPrice()).setScale(2, RoundingMode.DOWN);
                if (s.compareTo(new BigDecimal(0)) == 0) {
                    p.getInventory().addItem(new ItemStack(Material.GOLD_ORE, num_sold - i));
                    p.sendMessage(Utils.chat("&c" + currency.name() + " &cis worthless!!!"));
                    break;
                }
                plugin.getLogger().info(p.getDisplayName() + " sold item for " + s);
                give(p, s);
            }
            BigDecimal diff = plugin.getEcoAPI().getAccount(p.getUniqueId()).getHoldings().subtract(before);
            p.sendMessage(Utils.chat("&2Sold $&6" + diff + " &2of &5" + currency.name() + " &2to the server!"));
        } else {
            p.sendMessage(Utils.chat("&cOnly &5" + currency.name() + " &ccan be sold to the server!"));
        }
    }

    /**
     * Player requests a quantity of money be given to another player or
     * themselves and the specified Player receives the quantity of money.
     * Sub-command of /dyneconomy.
     *
     * @param p Player to give money to
     * @param args index 1 contains the amount of money
     */
    void giveCommand(Player p, String[] args) {
        if (Array.getLength(args) > 2) {
            Player s = Bukkit.getPlayerExact(args[1]);
            if (s == null) {
                p.sendMessage(Utils.chat("&cCorrect usage: &f/dyneconomy give [player] [amount]"));
            } else {
                BigDecimal floor = BigDecimal.valueOf(Double.parseDouble(args[2])).setScale(2, RoundingMode.DOWN);
                give(s, floor);
                p.sendMessage(Utils.chat("&2Gave player &5" + args[1] + " $" + floor));
            }
        } else {
            p.sendMessage(Utils.chat("&cCorrect usage: &f/dyneconomy give [player] [amount]"));
        }
    }

    /**
     * Give a Player a given amount of money.
     *
     * @param p Player to give money to
     * @param amount Quantity of money to give player
     */
    void give(Player p, BigDecimal amount) {
        Account a = plugin.getEcoAPI().getAccount(p.getUniqueId());
        a.addHoldings(amount);
        plugin.addTotalMoney(amount);
    }

    /**
     * Parse Player command and execute corresponding sub-command. This method
     * is called whenever a command beginning with /de or /dyneconomy is run.
     *
     * @param sender source of the command
     * @param cmd Command which was executed
     * @param label alias of the command which was used
     * @param args passed command arguments
     * @return true if a valid command, otherwise false
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLogger().warning("Only players can use this command!");
            return false;
        } else {
            Player p = (Player) sender;
            if (Array.getLength(args) > 0) {
                if (args[0].equalsIgnoreCase("sell")) {
                    if (p.hasPermission("dyneco.player") || p.hasPermission("dyneco.admin")) {
                        sellCommand(p);
                    } else {
                        p.sendMessage(Utils.chat("&cYou don't have permission to do that!"));
                    }
                } else if (args[0].equalsIgnoreCase("give")) {
                    if (p.hasPermission("dyneco.admin")) {
                        giveCommand(p, args);
                    } else {
                        p.sendMessage(Utils.chat("&cYou don't have permission to do that!"));
                    }
                } else if (args[0].equalsIgnoreCase("resync")) {
                    if (p.hasPermission("dyneco.admin")) {
                        plugin.syncMaxMoney();
                        plugin.syncTotalMoney();
                    } else {
                        p.sendMessage(Utils.chat("&cYou don't have permission to do that!"));
                    }
                } else if (args[0].equalsIgnoreCase("townshop")) {
                    p.sendMessage(Utils.chat("&cNot implemented yet!"));
                } else {
                    p.sendMessage(Utils.chat("&cI don't know that command!"));
                }
            } else {
                if (p.hasPermission("dyneco.player") || p.hasPermission("dyneco.admin")) {
                    infoCommand(p);
                } else {
                    p.sendMessage(Utils.chat("&cYou don't have permission to do that!"));
                }
            }
            return true;
        }
    }
}