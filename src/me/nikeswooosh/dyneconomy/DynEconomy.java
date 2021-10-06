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

    private static final String DATA_FILENAME = "data.yml";

    private TownyUniverse townyUniverse = null;
    private BigDecimal total_money = new BigDecimal(0.00);
    private double calc_max_money = 0;
    private YamlConfiguration townyConfig = null;
    private TNEAPI ecoAPI = null;

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
        return  townyConfig;
    }

    public TNEAPI getEcoAPI() {
        return ecoAPI;
    }

    public BigDecimal getTotalMoney() {
        return total_money;
    }

    public TownyUniverse getTownyUniverse() {
        return townyUniverse;
    }

    public double getMaxMoney() {
        return calc_max_money;
    }

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

    public void syncTotalMoney() {
        BigDecimal total = new BigDecimal(0.00);
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

    public void syncMaxMoney() {
        double max = getConfig().getDouble("base_max_money");
        max += TownyAPI.getInstance().getTownyWorld(getConfig()
                .getString("world_name")).getTownBlocks().size() *
                getConfig().getDouble("money_per_chunk");
        max += townyUniverse.getResidents().size() * getConfig().getDouble("money_per_player");
        calc_max_money = max;
    }

    public void addTotalMoney(BigDecimal amount) {
        total_money = total_money.add(amount);
    }

    public void addMaxMoney(double amount) {
        calc_max_money += amount;
    }
}
