within ;
model VarTest
  Modelica.Blocks.Interfaces.RealInput SigIn1
    annotation (Placement(transformation(extent={{-80,60},{-40,100}})));
  Modelica.Blocks.Interfaces.RealInput SigIn2
    annotation (Placement(transformation(extent={{-80,20},{-40,60}})));
  Modelica.Blocks.Interfaces.RealInput SigIn3
    annotation (Placement(transformation(extent={{-80,-18},{-40,22}})));
  Modelica.Blocks.Interfaces.RealOutput SigOut1
    annotation (Placement(transformation(extent={{40,70},{60,90}})));
  Modelica.Blocks.Interfaces.RealOutput SigOut2
    annotation (Placement(transformation(extent={{40,30},{60,50}})));
  Modelica.Blocks.Interfaces.RealOutput SigOut3
    annotation (Placement(transformation(extent={{40,-8},{60,12}})));
  Modelica.Blocks.Math.Gain gain(k=3)
    annotation (Placement(transformation(extent={{-24,-50},{-4,-30}})));
  Modelica.Blocks.Math.Add add
    annotation (Placement(transformation(extent={{10,-30},{30,-10}})));
  Modelica.Blocks.Interfaces.RealOutput SigOut4
    annotation (Placement(transformation(extent={{40,-30},{60,-10}})));
  Modelica.Blocks.Interfaces.RealOutput SigOut5
    annotation (Placement(transformation(extent={{40,-50},{60,-30}})));
  Modelica.Blocks.Interfaces.RealOutput SigOut6
    annotation (Placement(transformation(extent={{40,-70},{60,-50}})));
  Modelica.Blocks.Math.Product product
    annotation (Placement(transformation(extent={{10,-70},{30,-50}})));
  Modelica.Blocks.Continuous.Integrator integrator
    annotation (Placement(transformation(extent={{10,-98},{30,-78}})));
  Modelica.Blocks.Interfaces.RealOutput SigOut7
    annotation (Placement(transformation(extent={{40,-98},{60,-78}})));
equation
  connect(SigIn1, SigOut1)
    annotation (Line(points={{-60,80},{50,80}}, color={0,0,127}));
  connect(SigIn2, SigOut2)
    annotation (Line(points={{-60,40},{-8,40},{50,40}}, color={0,0,127}));
  connect(SigIn3, SigOut3)
    annotation (Line(points={{-60,2},{-60,2},{50,2}}, color={0,0,127}));
  connect(add.y, SigOut4)
    annotation (Line(points={{31,-20},{50,-20}}, color={0,0,127}));
  connect(add.u1, SigOut1) annotation (Line(points={{8,-14},{0,-14},{0,80},{50,
          80}}, color={0,0,127}));
  connect(add.u2, SigOut2) annotation (Line(points={{8,-26},{-10,-26},{-10,40},
          {50,40}}, color={0,0,127}));
  connect(gain.u, SigOut3) annotation (Line(points={{-26,-40},{-30,-40},{-30,2},
          {50,2}}, color={0,0,127}));
  connect(gain.y, SigOut5)
    annotation (Line(points={{-3,-40},{24,-40},{50,-40}}, color={0,0,127}));
  connect(SigOut6, product.y)
    annotation (Line(points={{50,-60},{31,-60}}, color={0,0,127}));
  connect(product.u1, SigOut4) annotation (Line(points={{8,-54},{4,-54},{4,-34},
          {34,-34},{34,-20},{50,-20}}, color={0,0,127}));
  connect(gain.y, product.u2) annotation (Line(points={{-3,-40},{0,-40},{0,-66},
          {8,-66}}, color={0,0,127}));
  connect(integrator.u, product.y) annotation (Line(points={{8,-88},{-2,-88},{
          -2,-72},{34,-72},{34,-60},{31,-60}}, color={0,0,127}));
  connect(integrator.y, SigOut7)
    annotation (Line(points={{31,-88},{50,-88},{50,-88}}, color={0,0,127}));
  annotation (uses(Modelica(version="3.2.1")));
end VarTest;
