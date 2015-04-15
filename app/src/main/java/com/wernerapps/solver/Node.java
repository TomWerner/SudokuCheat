package com.wernerapps.solver;

public class Node
{
    // Number of nodes in this column if its a header, otherwise row
    public int  s;
    // Column number
    public int  c;
    public Node left;
    public Node right;
    public Node up;
    public Node down;

    public Node(Node l, Node d, int s1, int c1)
    {
        s = s1;
        c = c1;

        if (l == null)
        {
            left = this;
            right = this;
        }
        else
        {
            left = l.left;
            right = l;
            left.right = this;
            right.left = this;
        }

        if (d == null)
        {
            up = this;
            down = this;
        }
        else
        {
            up = d.up;
            down = d;
            up.down = this;
            down.up = this;
        }
    }

    public void hideVertical()
    {
        up.down = down;
        down.up = up;
    }

    public void unhideVertical()
    {
        down.up = this;
        up.down = this;
    }

    public void hideHoriz()
    {
        right.left = left;
        left.right = right;
    }

    public void unhideHoriz()
    {
        left.right = this;
        right.left = this;
    }
}
