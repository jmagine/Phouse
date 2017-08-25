/*-----------------------------------------------------------------------------

                                                         Author: Jason Ma
                                                         Date:   Oct 01 2016
                                      Phouse

  File Name:      driver.java
  Description:    Drives program.
-----------------------------------------------------------------------------*/

/*-----------------------------------------------------------------------------
  Class name: driver
  File:         driver.java
  
  Description: Drives program.
-----------------------------------------------------------------------------*/
public class driver {

  /*---------------------------------------------------------------------------
    Routine Name: main
    File:         driver.java
  
    Description: Driver for program, delegates to ComputerPort
  
    Parameter Descriptions:
    name               description
    ------------------ -----------------------------------------------
    args               command line args, currently not used
  ---------------------------------------------------------------------------*/
	public static void main(String[] args) {
		ComputerPort cp = new ComputerPort();
		cp.updateState();
	}
}
