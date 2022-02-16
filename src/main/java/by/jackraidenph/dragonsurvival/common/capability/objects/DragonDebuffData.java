package by.jackraidenph.dragonsurvival.common.capability.objects;

public class DragonDebuffData
{
	public double timeWithoutWater;
	public int timeInRain;
	public int timeInDarkness;
	
	public DragonDebuffData(double timeWithoutWater, int timeInDarkness, int timeInRain)
	{
		this.timeWithoutWater = timeWithoutWater;
		this.timeInDarkness = timeInDarkness;
		this.timeInRain = timeInRain;
	}
}
