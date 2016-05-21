/**
 *
 */
package adhoc.voip;

// TODO: Auto-generated Javadoc
/**
 * The Class Profile.
 * Represents a single personal profile.
 *
 * @author Raz and Elad
 */
public class Profile {

    /** The name. */
    public String name;

    /** The country. */
    public String country;

    /** The city. */
    public String city;

    /** The email. */
    public String email;

    /** The year. */
    public int year;

    /** The month. */
    public int month;

    /** The day. */
    public int day;

    /** The female. */
    public boolean female;

    /** The single. */
    public boolean single;

    /**
     * Instantiates a new profile.
     */
    public Profile() {
        this.name = "Me";
        this.country = "Israel";
        this.city = "Beer Sheva";
        this.email = "";
        this.year = 1980;
        this.month = 5;
        this.day = 1;
        this.female = true;
        this.single = true;
    }

    /**
     * Instantiates a new profile.
     *
     * @param name the name
     * @param country the country
     * @param city the city
     * @param email the email
     * @param year the year
     * @param month the month
     * @param day the day
     * @param female the female
     * @param single the single
     */
    public Profile(String name, String country, String city, String email, int year, int month, int day, boolean female , boolean single) {
        this.name = name;
        this.country = country;
        this.city = city;
        this.email = email;
        this.year = year;
        this.month = month;
        this.day = day;
        this.female = female;
        this.single = single;
    }

    /**
     * Sets the profile.
     *
     * @param toSetProfile the new profile
     */
    public void setProfile( Profile toSetProfile)
    {
        if(toSetProfile!= null)
        {
            this.name = toSetProfile.name;
            this.country = toSetProfile.country;
            this.city = toSetProfile.city;
            this.email = toSetProfile.email;
            this.year = toSetProfile.year;
            this.month = toSetProfile.month;
            this.day = toSetProfile.day;
            this.female = toSetProfile.female;
            this.single = toSetProfile.single;
        }
    }
}
