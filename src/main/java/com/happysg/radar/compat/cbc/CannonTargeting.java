package com.happysg.radar.compat.cbc;

import com.happysg.radar.compat.vs2.VS2Utils;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.analysis.solvers.*;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.LoadedShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Double.NaN;
import static java.lang.Math.log;
import static java.lang.Math.toRadians;

public class CannonTargeting {
    public static double calculateProjectileYatX(double speed, double dX, double thetaRad,double drag, double g ) {
        double log = log(1 - (drag * dX) / (speed * Math.cos(thetaRad)));
        if (Double.isInfinite(log)) log = NaN;
        return dX * Math.tan(thetaRad) + (dX * g) / (drag * speed * Math.cos(thetaRad)) + g*log / (drag * drag);
    }

    public static List<Double> calculatePitch(double speed, Vec3 targetPos, Vec3 originPos, int barrelLength, double drag, double gravity) {
        if (speed == 0) {
            return null;
        }
        double d1 = targetPos.x - originPos.x;
        double d2 = targetPos.z - originPos.z;
        double distance = Math.abs(Math.sqrt(d1 * d1 + d2 * d2));
        double d3 = targetPos.y - originPos.y;
        double g = Math.abs(gravity);
        UnivariateFunction diffFunction = theta -> {
            double thetaRad = toRadians(theta);

            double dX = distance - (Math.cos(thetaRad) * (barrelLength));
            double dY = d3 - (Math.sin(thetaRad) * (barrelLength));
            double y = calculateProjectileYatX(speed, dX, thetaRad, drag, g);
            return y - dY;
        };

        UnivariateSolver solver = new BrentSolver(1e-32);

        double start = -90;
        double end = 90;
        double step = 1.0;
        List<Double> roots = new ArrayList<>();

        double prevValue = diffFunction.value(start);
        double prevTheta = start;
        for (double theta = start + step; theta <= end; theta += step) {
            double currValue = diffFunction.value(theta);
            if (prevValue * currValue < 0) {
                try {
                    double root = solver.solve(1000, diffFunction, prevTheta, theta);
                    roots.add(root);
                } catch (Exception e) {
                    return null;
                }
            }
            prevTheta = Double.isNaN(currValue) ? prevTheta : theta;
            prevValue = Double.isNaN(currValue) ? prevValue : currValue;
        }
        if (roots.isEmpty()) {
            return null;
        }
        return roots;
    }

    public static List<Double> calculatePitch(CannonMountBlockEntity mount, Vec3 targetPos, ServerLevel level) {
        if (mount == null || targetPos == null) {
            return null;
        }

        PitchOrientedContraptionEntity contraption = mount.getContraption();
        if ( contraption == null || !(contraption.getContraption() instanceof AbstractMountedCannonContraption cannonContraption)) {
            return null;
        }
        float chargePower = CannonUtil.getInitialVelocity(cannonContraption, level);

        Vec3 mountPos = VS2Utils.getWorldVec(level,mount.getBlockPos().above(2).getCenter());
        int barrelLength = CannonUtil.getBarrelLength(cannonContraption);

        double drag = CannonUtil.getProjectileDrag(cannonContraption, level);
        double gravity = CannonUtil.getProjectileGravity(cannonContraption, level);

        return calculatePitch(chargePower, VS2Utils.getWorldVec(level,targetPos), mountPos, barrelLength, drag, gravity);
    }

    public static List<List<Double>> calculatePitchAndYawVS2(CannonMountBlockEntity mount, Vec3 targetPos, ServerLevel level) {
        if (mount == null || targetPos == null) {
            return null;
        }

        PitchOrientedContraptionEntity contraption = mount.getContraption();
        if ( contraption == null || !(contraption.getContraption() instanceof AbstractMountedCannonContraption cannonContraption)) {
            return null;
        }
        float chargePower = CannonUtil.getInitialVelocity(cannonContraption, level);

        Vec3 mountPos = mount.getBlockPos().above(2).getCenter();
        int barrelLength = CannonUtil.getBarrelLength(cannonContraption);

        double drag = CannonUtil.getProjectileDrag(cannonContraption, level);
        double gravity = CannonUtil.getProjectileGravity(cannonContraption, level);

        return calculatePitchAndYawVS2(level, chargePower, targetPos, mountPos, barrelLength, drag, gravity);
    }


    public static List<List<Double>> calculatePitchAndYawVS2(Level level, double speed, Vec3 targetPos, Vec3 mountPos, int barrelLength, double drag, double gravity){
        LoadedShip ship = VSGameUtilsKt.getShipObjectManagingPos(level, mountPos.x, mountPos.y, mountPos.z);
        if (ship == null) {
            System.out.println("null");
            return null;
        }
        Vector3d eulerAngles = new Vector3d();
        ship.getTransform().getShipToWorldRotation().getEulerAnglesXYZ(eulerAngles);
        double initialTheta = eulerAngles.z;
        double initialZeta = eulerAngles.y;
        initialTheta = Math.toDegrees(initialTheta);
        initialZeta = Math.toDegrees(initialZeta);
        initialZeta += 90;
        initialTheta = -initialTheta;
        //initialTheta += 90;
        mountPos = VS2Utils.getWorldVec(level, mountPos);
        VS2TargetingSolver targetingSolver = new VS2TargetingSolver(speed, drag, gravity, barrelLength, mountPos, targetPos, initialTheta, initialZeta);
        return null;
    }
}