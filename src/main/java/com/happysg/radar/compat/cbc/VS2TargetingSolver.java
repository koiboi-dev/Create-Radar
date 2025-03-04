package com.happysg.radar.compat.cbc;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.*;
import org.apache.commons.math3.optim.nonlinear.scalar.*;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.*;
import org.apache.commons.math3.random.RandomVectorGenerator;
import org.joml.Matrix4dc;
import org.valkyrienskies.core.api.ships.Ship;

import java.util.*;

import static com.happysg.radar.compat.cbc.CannonUtil.isUp;
import static com.happysg.radar.compat.vs2.VS2Utils.getVec3FromVector;
import static com.happysg.radar.compat.vs2.VS2Utils.getVector3dFromVec3;
import static java.lang.Math.*;

public class VS2TargetingSolver {
    private final double u;
    private final double drag;
    private final Vec3 targetPos;
    private final Vec3 mountPos;
    private final double g;
    private final Level level;
    double initialTheta;
    double initialZeta;
    double initialPsi;
    double l;
    Matrix4dc shipToWorld;
    Matrix4dc worldToShip;

    private static final double TOLERANCE = 1e-3;


    // Constructor to set up the known values
    public VS2TargetingSolver(Level level, double u, double drag, double g, double barrelLength, Vec3 mountPos, Vec3 targetPos, double initialTheta, double initialZeta, double initialPsi, Ship ship ) {
        this.level = level;
        this.u = u;
        this.drag = drag;
        this.g = abs(g);
        this.initialTheta = initialTheta;
        this.initialZeta = initialZeta;
        this.initialPsi = initialPsi;
        this.shipToWorld = ship.getShipToWorld();
        this.worldToShip = ship.getWorldToShip();
        this.targetPos = targetPos;
        this.mountPos = mountPos; // shipyard coord
        this.l = barrelLength;


    }

    private MultivariateFunction createFunction() { //TODO vypočítat čas z ZED a pak použít na vypočet y a x
        return point -> {
//            Vec3 shipyardFrontOfBarrel = getFrontOfBarrel(level, mountPos);
//            if(shipyardFrontOfBarrel == null) return Double.POSITIVE_INFINITY;
            double theta = point[0];
            double zeta = point[1];
            double thetaRad = toRadians(theta);
            double zetaRad = toRadians(zeta);

            Vec3 shipyardFrontOfBarrel = mountPos.add(cos(zetaRad+PI/2)*cos(thetaRad)*l, sin(thetaRad)*l, sin(zetaRad+PI/2)*cos(thetaRad)*l);
            if(isUp(level, mountPos)){
                shipyardFrontOfBarrel = shipyardFrontOfBarrel.add(0,2.5,0);
            }
            else {
                shipyardFrontOfBarrel = shipyardFrontOfBarrel.add(0,-2.5,0);
            }
            Vec3 frontOfBarrel = getVec3FromVector(shipToWorld.transformPosition(getVector3dFromVec3(shipyardFrontOfBarrel)));
            Vec3 diffVec = targetPos.subtract(frontOfBarrel); // opravit barrel location
            double dZ = diffVec.z;
            double dY = diffVec.y;
            double dX = diffVec.x;

            double summedTheta = thetaRad+initialTheta;
            double summedZeta = zetaRad+initialZeta;
            theta = cos(initialPsi)*(summedTheta)-sin(initialPsi)*(summedZeta); // TODO opravit tohle
            zeta = sin(initialPsi)*(summedTheta)+cos(initialPsi)*(summedZeta); //TODO aj toto

            zetaRad = zeta;
            thetaRad = theta;

            zetaRad+=PI/2;

            double log = 1-(drag*dZ)/(u*cos(thetaRad)*sin(zetaRad));
            if(log <= 0) return Double.POSITIVE_INFINITY;
            double time = log(log)/-drag;
            if(time <= 0) return Double.POSITIVE_INFINITY;
            double dragDecay = (1-exp(-drag*time));
            double newX = u*cos(thetaRad)*cos(zetaRad)*dragDecay/drag;
            //double newY = (u*sin(thetaRad))*dragDecay/drag - g*time/drag;


            double newY = (drag*u*sin(thetaRad)+g)*dragDecay/(drag*drag) - g*time/drag;
            return abs(dY-newY)+abs(dX-newX);
        };
    }
    RandomVectorGenerator randomVectorGenerator = new RandomVectorGenerator() {
        private final Random random = new Random();
        @Override
        public double[] nextVector() {
            // Define your bounds:
            double thetaLower = -30;
            double thetaUpper = 60;
            double zetaLower = 0;
            double zetaUpper = 360;
            double theta = thetaLower + random.nextDouble() * (thetaUpper - thetaLower);
            double zeta = zetaLower + random.nextDouble() * (zetaUpper - zetaLower);
            return new double[]{theta, zeta};
        }
    };

    public List<List<Double>> solveThetaZeta() {
        int numStarts = 10; // Number of starting points for multi-start optimization.
        MultiStartMultivariateOptimizer optimizer = new MultiStartMultivariateOptimizer(
                new BOBYQAOptimizer(5), numStarts, randomVectorGenerator
        );

        // Define search bounds (theta in [-90, 90] and zeta in [0, 360] degrees).
        double[] lowerBounds = {-90, 0};
        double[] upperBounds = {90, 360};
        try {
            optimizer.optimize(
                    new MaxEval(1000),
                    new ObjectiveFunction(createFunction()),
                    GoalType.MINIMIZE,
                    new InitialGuess(new double[]{0, 0}),  // initial guess (will be varied in multi-start)
                    new SimpleBounds(lowerBounds, upperBounds)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        PointValuePair[] optima = optimizer.getOptima();
        List<List<Double>> results = new ArrayList<>();
        Set<String> uniqueSolutions = new HashSet<>();
        for (PointValuePair opt : optima) {
            if (opt == null) continue;
            double error = opt.getValue();
            if (error < TOLERANCE) {
                double[] point = opt.getPoint();
                double theta = point[0];
                double zeta = point[1];
                // Use a key with rounding to filter out duplicate solutions.
                String key = String.format("%.3f_%.3f", theta, zeta);
                if (!uniqueSolutions.contains(key)) {
                    uniqueSolutions.add(key);
                    List<Double> pair = new ArrayList<>();
                    pair.add(theta);
                    pair.add(zeta);
                    results.add(pair);
                }
            }
        }
        return results;
    }
}
