package org.testremote;

class Loop extends Thread
{
    private Runnable r;
    
    Loop(Runnable r)
    {
        this.r = r;
    }
    
    @Override
    public void run()
    {
        for (;;)
        {
            r.run();
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                System.err.print(e);
            }
        }
    }
}
