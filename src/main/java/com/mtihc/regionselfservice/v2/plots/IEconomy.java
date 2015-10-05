package com.mtihc.regionselfservice.v2.plots;

import java.util.UUID;
import org.bukkit.OfflinePlayer;
import com.mtihc.regionselfservice.v2.plots.exceptions.EconomyException;

public interface IEconomy {
  void deposit(UUID accountHolder, double amount);

  void deposit(OfflinePlayer accountHolder, double amount);

  void withdraw(UUID accountHolder, double amount) throws EconomyException;

  void withdraw(OfflinePlayer accountHolder, double amount) throws EconomyException;

  double getBalance(UUID accountHolder);

  double getBalance(OfflinePlayer accountHolder);

  String format(double amount);

  String getName();
}
