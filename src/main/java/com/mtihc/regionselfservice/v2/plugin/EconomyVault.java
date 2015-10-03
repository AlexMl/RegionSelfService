package com.mtihc.regionselfservice.v2.plugin;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import com.mtihc.regionselfservice.v2.plots.IEconomy;
import com.mtihc.regionselfservice.v2.plots.exceptions.EconomyException;

public class EconomyVault implements IEconomy {
  private Economy econ;
  private Logger logger;

  public EconomyVault(Economy econ, Logger logger) {
    this.econ = econ;
    this.logger = logger;
  }

  public void deposit(UUID accountHolder, double amount) {
    deposit(Bukkit.getOfflinePlayer(accountHolder), amount);
  }

  public void deposit(OfflinePlayer accountHolder, double amount) {
    EconomyResponse res = this.econ.depositPlayer(accountHolder, amount);
    if (!res.transactionSuccess()) {
      this.logger.log(Level.WARNING,
          "Failed to deposit " + amount + " to " + accountHolder.getName() + ": " + res.errorMessage);
    }

  }

  public void withdraw(UUID accountHolder, double amount) throws EconomyException {
    withdraw(Bukkit.getOfflinePlayer(accountHolder), amount);
  }

  public void withdraw(OfflinePlayer accountHolder, double amount) throws EconomyException {
    EconomyResponse res = this.econ.withdrawPlayer(accountHolder, amount);
    if (!res.transactionSuccess()) {
      if (res.balance >= amount) {
        // it's not the balance
        this.logger.log(Level.WARNING,
            "Failed to withdraw " + amount + " from " + accountHolder.getName() + ": " + res.errorMessage);
      }
      throw new EconomyException(res.errorMessage);

    }

  }

  public double getBalance(UUID accountHolder) {
    return getBalance(Bukkit.getOfflinePlayer(accountHolder));
  }

  public double getBalance(OfflinePlayer accountHolder) {
    return this.econ.getBalance(accountHolder);
  }

  public String format(double amount) {
    return this.econ.format(amount);
  }

  public String getName() {
    return this.econ.getName();
  }
}
