import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

class Playground {
    public static void main(String[] args) {
        System.out.println("Java playground is ready.");

        System.out.println("Today: " + LocalDate.now());

        System.out.println(findMedianSortedArrays(new int[]{1, 3}, new int[]{2}));
        // Add and run standalone Java experiments here.
    }

    public static double findMedianSortedArrays(int[] nums1, int[] nums2) {
        int size  = nums1.length + nums2.length;
        int[] merged = new int[size];
        int i = 0, j = 0, k = 0;
        while (i < nums1.length && j < nums2.length) {
            if (nums1[i] < nums2[j]) {
                merged[k++] = nums1[i++];
            } else {
                merged[k++] = nums2[j++];
            }
        }
        while (i < nums1.length) {
            merged[k++] = nums1[i++];
        }
        while (j < nums2.length) {
            merged[k++] = nums2[j++];
        }
        if (size % 2 == 0) {
            return (merged[size / 2 - 1] + merged[size / 2]) / 2.0;
        } else {
            return merged[size / 2];
        }
    }
}
