package me.nikeswooosh.dyneconomy;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

import me.nikeswooosh.dyneconomy.commands.EconomyCommands;
import me.nikeswooosh.dyneconomy.listeners.TownyListener;
import net.tnemc.core.TNE;
import net.tnemc.core.common.api.TNEAPI;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

public class DynEconomy extends JavaPlugin {
    private TownyUniverse townyUniverse = null;
    private BigDecimal total_money = new BigDecimal(0.00);
    private double calc_max_money = 0;
    private YamlConfiguration townyConfig = null;
    private TNEAPI ecoAPI = null;

    /**
     * Load our configuration file, create our TNE API and Towny instances and
     * sync our plugin state with Towny and TNE.
     */
    @Override
    public void onEnable() {
        File townyFile = new File(Towny.getPlugin().getConfigPath());
        if (!townyFile.exists()) {
            getLogger().severe("Couldn't find Towny config.yml - Disabling plugin");
            getServer().getPluginManager().disablePlugin(this);
        }
        ecoAPI = TNE.instance().api();
        townyConfig = YamlConfiguration.loadConfiguration(townyFile);
        townyUniverse = TownyUniverse.getInstance();
        saveDefaultConfig();
        syncTotalMoney();
        syncMaxMoney();
        new EconomyCommands(this);
        new TownyListener(this);
    }

    public YamlConfiguration getTownyConfig() {
        return townyConfig;
    }

    public TNEAPI getEcoAPI() {
        return ecoAPI;
    }

    public BigDecimal getTotalMoney() {
        return total_money;
    }

    public double getMaxMoney() {
        return calc_max_money;
    }

    /**
     * Returns the selling price of a single unit of our currency item. We
     * evaluate this based on the current market cap and the values specified
     * in config.yml.
     *
     * @return the selling price per unit of our item
     */
    public double getUnitPrice() {
        double load_factor = getTotalMoney().divide(BigDecimal.valueOf(getMaxMoney()),
                RoundingMode.HALF_UP).doubleValue();
        double init_value = getConfig().getDouble("currency_init_value");
        double half_value = getConfig().getDouble("currency_half_value");
        if (load_factor < 0.5) {
            return Math.round(((init_value - (load_factor * (init_value - half_value) * 2))) * 100.0) / 100.0;
        } else if (load_factor > 1.0) {
            return 0;
        }   else {
            return Math.round((half_value * (1 - load_factor) * 2) * 100.0) / 100.0;
        }
    }

    /**
     * Calculates the total money stored in all town, nation, and player bank
     * accounts and updates the total_money field using that value.
     */
    public void syncTotalMoney() {
        BigDecimal total = new BigDecimal("0.00");
        Collection<Town> towns = townyUniverse.getTowns();
        for (Town t : towns) {
            total = total.add(BigDecimal.valueOf(t.getAccount().getHoldingBalance()));
        }
        Collection<Resident> residents = townyUniverse.getResidents();
        for (Resident r : residents) {
            total = total.add(ecoAPI.getAccount(r.getUUID()).getHoldings());
        }
        Collection<Nation> nations = townyUniverse.getNations();
        for (Nation n : nations) {
            total = total.add(BigDecimal.valueOf(n.getAccount().getHoldingBalance()));
        }
        total_money = total;
        getLogger().info("Updated total money to $" + total);
    }

    /**
     * Calculates the current maximum amount of money using the number of town
     * blocks claimed and total number of town residents. Updates the
     * calc_max_money field using the value.
     */
    public void syncMaxMoney() {
        double max = getConfig().getDouble("base_max_money");
        max += TownyAPI.getInstance().getTownyWorld(getConfig()
                .getString("world_name")).getTownBlocks().size() *
                getConfig().getDouble("money_per_chunk");
        max += townyUniverse.getResidents().size() * getConfig().getDouble("money_per_player");
        calc_max_money = max;
    }

    /**
     * Modifies the count of the total amount of money in circulation by a
     * given amount.
     *
     * @param amount a BigDecimal to increase the total money count by
     */
    public void addTotalMoney(BigDecimal amount) {
        total_money = total_money.add(amount);
    }

    /**
     * Modifies the count of the maximum amount of money by a given amount.
     *
     * @param amount a BigDecimal to increase the maximum money count by
     */
    public void addMaxMoney(double amount) {
        calc_max_money += amount;
    }
}
