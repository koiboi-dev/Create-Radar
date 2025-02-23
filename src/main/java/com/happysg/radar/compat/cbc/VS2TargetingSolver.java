package com.happysg.radar.compat.cbc;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.*;
import org.apache.commons.math3.optim.nonlinear.scalar.*;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.*;
import static java.lang.Math.*;

public class VS2TargetingSolver {
    private final double u;
    private final double drag;
    private final Vec3 targetPos;
    private final Vec3 originPos;
    private final double g;
    double initialTheta;
    double initialZeta;
    double l;

    // Constructor to set up the known values
    public VS2TargetingSolver(double u, double drag, double g, double barrelLength, Vec3 mountPos,  Vec3 targetPos, double initialTheta, double initialZeta) {
        this.u = u;
        this.drag = drag;
        this.g = g;
        this.initialTheta = initialTheta;
        this.initialZeta = initialZeta;
        this.targetPos = targetPos;
        this.originPos = mountPos;
        this.l = barrelLength;
    }

    private MultivariateFunction createFunction() {
        return point -> {
            Vec3 diffVec = targetPos.subtract(originPos);
            double theta = point[0];
            double zeta = point[1];
            double dZ = diffVec.z - (l*cos(theta)*sin(zeta)) - 1.5 * cos(initialTheta)*sin(initialZeta);
            double dY = diffVec.y - (l* sin(theta)) - 1.5 * sin(initialTheta);


            double thetaRad = toRadians(theta);
            double zetaRad = toRadians(zeta);

            double a = (drag * dZ) / (u * cos(thetaRad) * cos(zetaRad));
            double logTerm = log(a);
            double firstTerm = (u * sin(thetaRad) / drag) * (1 - a);
            double secondTerm = (g / (drag * drag)) * logTerm;

            return dY - (firstTerm - secondTerm);
        };
    }

    public double[] solveThetaZeta() {
        MultivariateOptimizer optimizer = new PowellOptimizer(1e-8, 1e-6);

        double thetaGuess = 0;
        double zetaGuess = 0;

        PointValuePair result = optimizer.optimize(
                new MaxEval(1000),
                new ObjectiveFunction(createFunction()),
                GoalType.MINIMIZE,
                new InitialGuess(new double[]{thetaGuess, zetaGuess}) // Initial guesses
        );

        return result.getPoint();
    }
}
