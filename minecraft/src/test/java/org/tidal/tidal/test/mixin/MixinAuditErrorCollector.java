/*
 * This file is part of Tidal.
 * Tidal - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2026 Tidal.
 *   <https://tidal.org> <https://github.com/Tidal/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   Tidal is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to Tidal, as published by Tidal,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to Tidal, as published by Tidal. If not, see
 * <https://tidal.org/legal/tidal/additional-terms>
 */

package cc.tidal.tidal.test.mixin;

import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinErrorHandler;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public final class MixinAuditErrorCollector implements IMixinErrorHandler {

    private static final List<Failure> FAILURES = new ArrayList<>();

    public MixinAuditErrorCollector() {
    }

    @Override
    public ErrorAction onPrepareError(IMixinConfig config, Throwable th, IMixinInfo mixin, ErrorAction action) {
        record("PREPARE", config.getName(), mixin.getClassName(), null, th);
        return ErrorAction.WARN;
    }

    @Override
    public ErrorAction onApplyError(String targetClassName, Throwable th, IMixinInfo mixin, ErrorAction action) {
        record("APPLY", mixin.getConfig().getName(), mixin.getClassName(), targetClassName, th);
        return ErrorAction.WARN;
    }

    static synchronized void record(String phase, String configName, String mixinName, String targetName, Throwable cause) {
        FAILURES.add(new Failure(phase, configName, mixinName, targetName, cause));
    }

    static synchronized List<Failure> failures() {
        return List.copyOf(FAILURES);
    }

    static synchronized void reset() {
        FAILURES.clear();
    }

    record Failure(String phase, String configName, String mixinName, String targetName, Throwable cause) {

        String render() {
            StringWriter out = new StringWriter();
            PrintWriter writer = new PrintWriter(out);
            writer.println("  phase  : " + phase);
            writer.println("  config : " + configName);
            writer.println("  mixin  : " + mixinName);
            writer.println("  target : " + (targetName == null ? "<n/a>" : targetName));
            if (cause == null) {
                writer.println("  cause  : <none>");
            } else {
                writer.println("  cause  :");
                cause.printStackTrace(writer);
            }
            writer.flush();
            return out.toString();
        }
    }
}
