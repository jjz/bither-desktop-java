/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bither.utils;

import net.bither.bitherj.core.Address;
import net.bither.bitherj.crypto.ECKey;
import net.bither.bitherj.crypto.PasswordSeed;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.utils.PrivateKeyUtil;
import net.bither.bitherj.utils.Utils;
import net.bither.model.Check;
import net.bither.model.Check.ICheckAction;
import net.bither.runnable.CheckRunnable;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CheckUtil {
    private CheckUtil() {

    }

    public static ExecutorService runChecks(List<Check> checks, int threadCount) {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (Check check : checks) {
            executor.execute(new CheckRunnable(check));
        }
        return executor;
    }


    public static Check initCheckForPrivateKey(
            final Address address, final SecureCharSequence password) {
        String title = String.format(LocaliserUtils.getString("check.address.private.key.title"), address
                .getShortAddress());
        Check check = new Check(title, new ICheckAction() {

            @Override
            public boolean check() {
                boolean result = new PasswordSeed(address).checkPassword(password);
                if (!result) {
                    try {
                        ECKey eckeyFromBackup = BackupUtil.getEckeyFromBackup(
                                address.getAddress(), password);
                        if (eckeyFromBackup != null) {
                            String encryptPrivateKey = PrivateKeyUtil.getEncryptedString(eckeyFromBackup);
                            if (!Utils.isEmpty(encryptPrivateKey)) {
                                address.setEncryptPrivKey(encryptPrivateKey);
                                address.savePrivateKey();
                                result = true;
                            }
                            eckeyFromBackup.clearPrivateKey();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        password.wipe();
                    }


                }
                return result;
            }
        });
        return check;
    }

    public static Check initCheckForRValue(final Address address) {
        String title = String.format(LocaliserUtils.getString("rcheck.address.title"), address.getShortAddress());
        Check check = new Check(title, new ICheckAction() {

            @Override
            public boolean check() {
                TransactionsUtil.completeInputsForAddress(address);
                return address.checkRValues();
            }
        });
        return check;
    }
}
