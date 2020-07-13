package com.avanipatel9.maps_avani_c0772788;

import com.google.android.gms.maps.model.LatLng;

import java.util.Arrays;
import java.util.Vector;

public class PointPlotter {
    public static int orientation(LatLng p1, LatLng p2,
                                  LatLng p3)
    {

        double val = (p2.longitude - p1.longitude) * (p3.latitude - p2.latitude) -
                (p2.latitude - p1.latitude) * (p3.longitude - p2.longitude);

        if (val == 0) return 0;  // colinear

        // clock or counterclock wise
        return (val > 0)? 1: 2;
    }
    public static Vector<LatLng> convexHull(LatLng markers[], int n)
    {
        // Initialize Result
        Vector<LatLng> hull = new Vector<LatLng>();

        // There must be at least 3 points
        if (n < 3){
            hull.addAll(Arrays.asList(markers));
            return hull;
        }



        // Find the leftmost point
        int l = 0;
        for (int i = 1; i < n; i++)
            if (markers[i].latitude < markers[l].latitude)
                l = i;

        // Start from leftmost point, keep moving
        // counterclockwise until reach the start point
        // again. This loop runs O(h) times where h is
        // number of points in result or output.
        int p = l, q;
        do
        {
            // Add current point to result
            hull.add(markers[p]);

            // Search for a point 'q' such that
            // orientation(p, x, q) is counterclockwise
            // for all points 'x'. The idea is to keep
            // track of last visited most counterclock-
            // wise point in q. If any point 'i' is more
            // counterclock-wise than q, then update q.
            q = (p + 1) % n;

            for (int i = 0; i < n; i++)
            {
                // If i is more counterclockwise than
                // current q, then update q
                if (orientation(markers[p], markers[i], markers[q])
                        == 2)
                    q = i;
            }

            // Now q is the most counterclockwise with
            // respect to p. Set p as q for next iteration,
            // so that q is added to result 'hull'
            p = q;

        } while (p != l);  // While we don't come to first
        // point

        // Print Result
        return hull;
    }
}
